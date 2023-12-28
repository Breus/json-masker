package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.util.AsciiCharacter;
import dev.blaauwendraad.masker.json.util.AsciiJsonUtil;
import dev.blaauwendraad.masker.json.util.FixedLengthTargetValueMaskUtil;
import dev.blaauwendraad.masker.json.util.Utf8Util;

import static dev.blaauwendraad.masker.json.util.AsciiCharacter.isDoubleQuote;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.isEscapeCharacter;

/**
 * {@link JsonMasker} that is optimized to mask the JSON properties for one or multiple target keys. This is the default
 * {@link JsonMasker} implementation.
 */
public final class KeyContainsMasker implements JsonMasker {
    /**
     * We are looking for targeted JSON keys with a maskable JSON value, so the closing quote can appear at minimum 3
     * characters till the end of the JSON in the following minimal case: '{"":1}'
     */
    private static final int MIN_OFFSET_JSON_KEY_QUOTE = 3;
    /**
     * Minimum JSON for which masking could be required is: {"":""}, so minimum length at least 7 bytes.
     */
    private static final int MIN_MASKABLE_JSON_LENGTH = 7;
    /**
     * Look-up trie containing the target keys.
     */
    private final ByteTrie targetKeysTrie;
    /**
     * The masking configuration for the JSON masking process.
     */
    private final JsonMaskingConfig maskingConfig;

    /**
     * Creates an instance of an {@link KeyContainsMasker}
     *
     * @param maskingConfig the masking configurations for the created masker
     */
    public KeyContainsMasker(JsonMaskingConfig maskingConfig) {
        this.maskingConfig = maskingConfig;
        this.targetKeysTrie = new ByteTrie(!maskingConfig.caseSensitiveTargetKeys());
        for (String key : maskingConfig.getTargetKeys()) {
            this.targetKeysTrie.insert(key);
        }
    }

    /**
     * Masks the values in the given input for all values having keys corresponding to any of the provided target keys.
     * This implementation is optimized for multiple target keys. Currently, only supports UTF_8 character encoding
     *
     * @param input the input message for which values might be masked
     * @return the masked message
     */
    @Override
    public byte[] mask(byte[] input) {
        /*
         * No masking required if input is not a JSON array or object (starting with either '{' or '['), or input is
         * shorter than the minimal maskable JSON input.
         */
        if (!isObjectOrArray(input) || input.length < MIN_MASKABLE_JSON_LENGTH) {
            return input;
        }
        /*
         * We can start the maskingState.currentIndex() at 1 since the first character can be skipped as it is either a '{' or '['.
         * This also ensures we can safely check for unescaped double quotes (without masking JSON string values).
         */
        MaskingState maskingState = new MaskingState(input, 1);
        mainLoop:
        while (maskingState.currentIndex() < maskingState.messageLength() - MIN_OFFSET_JSON_KEY_QUOTE) {
            // Find JSON strings by looking for unescaped double quotes
            while (!currentByteIsUnescapedDoubleQuote(maskingState)) {
                if (maskingState.currentIndex() < maskingState.messageLength() - MIN_OFFSET_JSON_KEY_QUOTE - 1) {
                    maskingState.incrementCurrentIndex();
                } else {
                    break mainLoop;
                }
            }
            int openingQuoteIndex = maskingState.currentIndex();
            maskingState.incrementCurrentIndex(); // step over the JSON key opening quote
            while (!currentByteIsUnescapedDoubleQuote(maskingState)
                    && maskingState.currentIndex() < maskingState.messageLength() - 1) {
                if (maskingState.currentIndex() < maskingState.messageLength() - MIN_OFFSET_JSON_KEY_QUOTE) {
                    maskingState.incrementCurrentIndex();
                } else {
                    break mainLoop;
                }
            }
            int closingQuoteIndex = maskingState.currentIndex();
            maskingState.incrementCurrentIndex(); // Step over the closing quote.

            /*
             * At this point, we found a JSON string ("...").
             * Now let's verify it is a JSON key (it must be followed by a colon with some white spaces between
             * the string value and the colon).
             */
            while (!AsciiCharacter.isColon(maskingState.byteAtCurrentIndex())) {
                if (!AsciiJsonUtil.isWhiteSpace(maskingState.byteAtCurrentIndex())) {
                    // The found string was not a JSON key, continue looking from where we left of.
                    continue mainLoop;
                }
                maskingState.incrementCurrentIndex();
            }

            /*
             * At this point, we found a string which is in fact a JSON key.
             * Now let's verify that the value is maskable (a number, string, array or object).
             */
            maskingState.incrementCurrentIndex(); //  The current index is at the colon between the key and value, step over the colon.
            skipWhitespaceCharacters(maskingState); // Step over all white characters after the colon,
            // Depending on the masking configuration, Strings, Numbers, Arrays and/or Objects should be masked.
            if (!isStartOfMaskableValue(maskingState)) {
                // The JSON key found did not have a maskable value, continue looking from where we left of.
                continue mainLoop;
            }

            /*
             * At this point, we found a JSON key with a maskable value, which is either a string, number, array,
             * or object. Now let's verify the found JSON key is a target key.
             */
            int keyLength = closingQuoteIndex - openingQuoteIndex - 1; // minus one for the quote
            boolean keyMatched = targetKeysTrie.search(
                    maskingState.getMessage(),
                    openingQuoteIndex + 1, // plus one for the opening quote
                    keyLength
            );
            if (maskingConfig.isInAllowMode() && keyMatched) {
                skipAllValues(maskingState); // the value belongs to a JSON key which is explicitly allowed, so skip it
                continue;
            }
            if (maskingConfig.isInMaskMode() && !keyMatched) {
                continue mainLoop; // The found JSON key is not a target key, so continue looking from where we left of.
            }

            /*
             * At this point, we found a targeted JSON key with a maskable value.
             * Now let's mask the value.
             */
            if (AsciiJsonUtil.isArrayStart(maskingState.byteAtCurrentIndex())) {
                maskArrayValueInPlace(maskingState);
            } else if (AsciiJsonUtil.isObjectStart(maskingState.byteAtCurrentIndex())) {
                maskObjectValueInPlace(maskingState);
            } else if (maskingConfig.isNumberMaskingEnabled()
                    && AsciiJsonUtil.isNumericCharacter(maskingState.byteAtCurrentIndex())) {
                maskNumberValueInPlace(maskingState);
            } else {
                // This block deals with masking strings target values.
                maskStringValueInPlace(maskingState);
            }
        }
        return maskingState.getMessage();
    }

    /**
     * Checks if the byte at the given index in the input byte array is an unescaped double quote character in UTF-8.
     *
     * @param maskingState the current masking state
     * @return whether the byte at index is an unescaped double quote
     */
    private static boolean currentByteIsUnescapedDoubleQuote(MaskingState maskingState) {
        return isDoubleQuote(maskingState.byteAtCurrentIndex())
                && !isEscapeCharacter(maskingState.byteAtCurrentIndexMinusOne());
    }

    private static boolean isObjectOrArray(byte[] input) {
        return AsciiCharacter.CURLY_BRACKET_OPEN.getAsciiByteValue() == input[0]
                || AsciiCharacter.SQUARE_BRACKET_OPEN.getAsciiByteValue() == input[0];
    }

    /**
     * Checks if the byte at the current index in the masking state is a white space character and if so, increments the
     * index by one. Returns whenever the byte at the current index in the masking state is not a white space
     * character.
     *
     * @param maskingState the current masking state
     */
    private static void skipWhitespaceCharacters(MaskingState maskingState) {
        while (AsciiJsonUtil.isWhiteSpace(maskingState.byteAtCurrentIndex())) {
            maskingState.incrementCurrentIndex();
        }
    }

    /**
     * Checks if the byte at the maskingState.currentIndex() is the start of a maskable value according to the provided
     * {@link JsonMaskingConfig}
     */
    private boolean isStartOfMaskableValue(MaskingState maskingState) {
        return isDoubleQuote(maskingState.byteAtCurrentIndex()) ||
                AsciiJsonUtil.isArrayStart(maskingState.byteAtCurrentIndex())
                || (maskingConfig.isNumberMaskingEnabled()
                && AsciiJsonUtil.isFirstNumberChar(maskingState.byteAtCurrentIndex())) || (
                AsciiJsonUtil.isObjectStart(maskingState.byteAtCurrentIndex()));
    }

    /**
     * Masks the string value in the provided input while starting from the provided current index which should be at
     * the opening quote of the string value.
     *
     * @param maskingState the current masking state where for which the current index must correspond to the opening
     *                     quote of the string value in the input array of the current index
     */
    private void maskStringValueInPlace(MaskingState maskingState) {
        maskingState.incrementCurrentIndex(); // step over the string value opening quote
        int targetValueLength = 0;
        int noOfEscapeCharacters = 0;
        int additionalBytesForEncoding = 0;
        boolean isEscapeCharacter = false;
        boolean previousCharacterCountedAsEscapeCharacter = false;
        while (!isDoubleQuote(maskingState.byteAtCurrentIndex()) || (isDoubleQuote(maskingState.byteAtCurrentIndex())
                && isEscapeCharacter)) {
            if (Utf8Util.getCodePointByteLength(maskingState.byteAtCurrentIndex()) > 1) {
                /*
                 * We only support UTF-8, so whenever code points are encoded using multiple bytes this should
                 * be represented by a single asterisk and the additional bytes used for encoding need to be
                 * removed.
                 */
                additionalBytesForEncoding += Utf8Util.getCodePointByteLength(maskingState.byteAtCurrentIndex()) - 1;
            }
            isEscapeCharacter =
                    isEscapeCharacter(maskingState.byteAtCurrentIndex()) && !previousCharacterCountedAsEscapeCharacter;
            if (isEscapeCharacter) {
                /*
                 * Non-escaped backslashes are escape characters and are not actually part of the string but
                 * only used for character encoding, so must not be included in the mask.
                 */
                noOfEscapeCharacters++;
                previousCharacterCountedAsEscapeCharacter = true;
            } else {
                if (previousCharacterCountedAsEscapeCharacter
                        && AsciiCharacter.isLowercaseU(maskingState.byteAtCurrentIndex())) {
                    /*
                     * The next 4 characters are hexadecimal digits which form a single character and are only
                     * there for encoding, so must not be included in the mask.
                     */
                    additionalBytesForEncoding += 4;
                }
                previousCharacterCountedAsEscapeCharacter = false;
            }
            maskingState.setByteAtCurrentIndex(AsciiCharacter.ASTERISK.getAsciiByteValue());
            targetValueLength++;
            maskingState.incrementCurrentIndex();
        }
        int obfuscationLength = maskingConfig.getObfuscationLength();
        if (obfuscationLength != -1
                && obfuscationLength != (targetValueLength - noOfEscapeCharacters)) {
            FixedLengthTargetValueMaskUtil.replaceTargetValueWithFixedLengthAsteriskMask(
                    maskingState,
                    obfuscationLength,
                    targetValueLength
            );
            /*
             * Compensate the maskingState.currentIndex() for shortening the input by setting fixed length to be at the closing
             * double quote of the string value.
             */
            maskingState.setCurrentIndex(maskingState.currentIndex() - (targetValueLength - obfuscationLength));
        } else if (noOfEscapeCharacters > 0 || additionalBytesForEncoding > 0) {
            // So we don't add asterisks for escape characters or additional encoding bytes (which
            // are not part of the String length)

            /*
             * The actual length of the string is the length minus escape characters (which are not part of the
             * string length). Also, unicode characters are denoted as 4-hex digits but represent actually
             * just one character, so for each of them 3 asterisks should be removed.
             */
            int actualStringLength = targetValueLength - noOfEscapeCharacters - additionalBytesForEncoding;
            FixedLengthTargetValueMaskUtil.replaceTargetValueWithFixedLengthAsteriskMask(
                    maskingState,
                    actualStringLength,
                    targetValueLength
            );
            /*
             * Compensate the maskingState.currentIndex() for shortening the input with noOfEscapeCharacters to be at closing
             * double quote of the String value.
             */
            maskingState.setCurrentIndex(
                    maskingState.currentIndex() - noOfEscapeCharacters - additionalBytesForEncoding);
        }
        maskingState.incrementCurrentIndex(); // step over closing quote of string value to start looking for the next JSON key.
    }

    /**
     * Masks the array of the masking state where the current index is on the opening square bracket
     *
     * @param maskingState the current masking state in which the array will be masked
     */
    private void maskArrayValueInPlace(MaskingState maskingState) {
        // This block deals with masking arrays
        int arrayDepth = 1;
        maskingState.incrementCurrentIndex(); // step over array opening square bracket
        skipWhitespaceCharacters(maskingState);
        while (arrayDepth > 0) {
            if (AsciiCharacter.isSquareBracketOpen(maskingState.byteAtCurrentIndex())) {
                arrayDepth++;
                maskingState.incrementCurrentIndex(); // step over opening bracket
            } else if (AsciiCharacter.isSquareBracketClose(maskingState.byteAtCurrentIndex())) {
                arrayDepth--;
                maskingState.incrementCurrentIndex(); // step over closing bracket
            } else if (AsciiCharacter.isDoubleQuote(maskingState.byteAtCurrentIndex())) {
                maskStringValueInPlace(maskingState); // mask string and step over it
            } else if (AsciiJsonUtil.isFirstNumberChar(maskingState.byteAtCurrentIndex())
                    && maskingConfig.isNumberMaskingEnabled()) {
                maskNumberValueInPlace(maskingState);
            } else if (AsciiJsonUtil.isObjectStart(maskingState.byteAtCurrentIndex())) {
                maskObjectValueInPlace(maskingState);
            } else {
                // non-maskable values
                skipAllValues(maskingState);
            }
            skipWhitespaceCharacters(maskingState);
            if (AsciiCharacter.isComma(maskingState.byteAtCurrentIndex())) {
                maskingState.incrementCurrentIndex(); // step over comma separating elements
            }
            skipWhitespaceCharacters(maskingState);
        }
    }

    /**
     * Masks all values (depending on the {@link JsonMaskingConfig} in the object.
     *
     * @param maskingState the current masking state
     */
    private void maskObjectValueInPlace(MaskingState maskingState) {
        maskingState.incrementCurrentIndex(); // step over opening curly bracket
        skipWhitespaceCharacters(maskingState);
        while (!AsciiCharacter.isCurlyBracketClose(maskingState.byteAtCurrentIndex())) {
            boolean valueMustBeMasked = true;
            if (maskingConfig.isInAllowMode()) {
                // In case target keys should be considered as allow list, we need to NOT mask certain keys
                int openingQuoteIndex = maskingState.currentIndex();
                maskingState.incrementCurrentIndex(); // step over the JSON key opening quote
                while (!currentByteIsUnescapedDoubleQuote(maskingState)) {
                    maskingState.incrementCurrentIndex();
                }
                int closingQuoteIndex = maskingState.currentIndex();
                int keyLength = closingQuoteIndex - openingQuoteIndex - 1; // minus one for the quote
                valueMustBeMasked = !targetKeysTrie.search(
                        maskingState.getMessage(),
                        openingQuoteIndex + 1, // plus one for the opening quote
                        keyLength
                );
            } else {
                maskingState.incrementCurrentIndex(); // step over the JSON key opening quote
                while (!currentByteIsUnescapedDoubleQuote(maskingState)) {
                    maskingState.incrementCurrentIndex(); // step over the JSON key content
                }
            }
            maskingState.incrementCurrentIndex();// step over the JSON key closing quote
            skipWhitespaceCharacters(maskingState);
            maskingState.incrementCurrentIndex(); // step over the colon ':'
            skipWhitespaceCharacters(maskingState);
            if (valueMustBeMasked) {
                if (AsciiCharacter.isSquareBracketOpen(maskingState.byteAtCurrentIndex())) {
                    maskArrayValueInPlace(maskingState);
                } else if (AsciiCharacter.isDoubleQuote(maskingState.byteAtCurrentIndex())) {
                    maskStringValueInPlace(maskingState); // mask string and step over it
                } else if (AsciiJsonUtil.isFirstNumberChar(maskingState.byteAtCurrentIndex())
                        && maskingConfig.isNumberMaskingEnabled()) {
                    maskNumberValueInPlace(maskingState);
                } else if (AsciiJsonUtil.isObjectStart(maskingState.byteAtCurrentIndex())) {
                    maskObjectValueInPlace(maskingState);
                } else {
                    while (!AsciiCharacter.isComma(maskingState.byteAtCurrentIndex())
                            && !AsciiCharacter.isCurlyBracketClose(
                            maskingState.byteAtCurrentIndex())) {
                        maskingState.incrementCurrentIndex(); // skip non-maskable value
                    }
                }
            } else {
                skipAllValues(maskingState);
            }
            skipWhitespaceCharacters(maskingState);
            if (AsciiCharacter.isComma(maskingState.byteAtCurrentIndex())) {
                maskingState.incrementCurrentIndex(); // step over comma separating elements
            }
            skipWhitespaceCharacters(maskingState);
        }
        maskingState.incrementCurrentIndex(); // step over closing curly bracket
    }

    private void maskNumberValueInPlace(MaskingState maskingState) {
        int obfuscationLength = maskingConfig.getObfuscationLength();
        // This block deals with numeric values
        int targetValueLength = 0;
        while (AsciiJsonUtil.isNumericCharacter(maskingState.byteAtCurrentIndex())) {
            targetValueLength++;
            maskingState.setByteAtCurrentIndex(AsciiCharacter.toAsciiByteValue(maskingConfig.getMaskNumericValuesWith()));
            /*
             * Following line cannot result in ArrayOutOfBound because of the early return after checking for
             * first char being a double quote.
             */
            maskingState.incrementCurrentIndex();
        }
        if (maskingConfig.isLengthObfuscationEnabled() && obfuscationLength != targetValueLength) {
            if (obfuscationLength == 0) {
                /*
                 * For obfuscation length 0, we want to obfuscate numeric values with a single 0 because an
                 * empty numeric value is illegal JSON.
                 */
                FixedLengthTargetValueMaskUtil.replaceTargetValueWithFixedLengthMask(
                        maskingState,
                        1,
                        targetValueLength,
                        AsciiCharacter.toAsciiByteValue(maskingConfig.getMaskNumericValuesWith())
                );
                /*
                 * The length of the input got changed, so we need to compensate that on our maskingState.currentIndex() by
                 * stepping the difference between value length and obfuscation length back.
                 */
                maskingState.setCurrentIndex(maskingState.currentIndex() - (targetValueLength - 1));
            } else {
                FixedLengthTargetValueMaskUtil.replaceTargetValueWithFixedLengthMask(
                        maskingState,
                        obfuscationLength,
                        targetValueLength,
                        AsciiCharacter.toAsciiByteValue(maskingConfig.getMaskNumericValuesWith())
                );
                /*
                 * The length of the input got changed, so we need to compensate that on our maskingState.currentIndex() by
                 * stepping the difference between value length and obfuscation length back.
                 */
                maskingState.setCurrentIndex(
                        maskingState.currentIndex() - (targetValueLength - obfuscationLength));
            }
        }
    }

    /**
     * This method assumes the masking state is currently at the first byte of a JSON value which can be any of: array,
     * boolean, object, null, number, or string and increments the current index in the masking state until the current
     * index is one position after the value.
     * <p>
     * Note: in case the value is an object or array, it skips the entire object and array and all the included elements
     * in it (e.g. nested arrays, objects, etc.)
     */
    private static void skipAllValues(MaskingState maskingState) {
        if (AsciiCharacter.isLowercaseN(maskingState.byteAtCurrentIndex())
                || AsciiCharacter.isLowercaseT(maskingState.byteAtCurrentIndex())) { // null and true
            maskingState.setCurrentIndex(maskingState.currentIndex() + 4);
        } else if (AsciiCharacter.isLowercaseF(maskingState.byteAtCurrentIndex())) { // false
            maskingState.setCurrentIndex(maskingState.currentIndex() + 5);
        } else if (AsciiCharacter.isDoubleQuote(maskingState.byteAtCurrentIndex())) { // strings
            skipStringValue(maskingState);
        } else if (AsciiJsonUtil.isFirstNumberChar(maskingState.byteAtCurrentIndex())) { // numbers
            while (AsciiJsonUtil.isNumericCharacter(maskingState.byteAtCurrentIndex())) {
                maskingState.incrementCurrentIndex();
            }
        } else if (AsciiCharacter.isCurlyBracketOpen(maskingState.byteAtCurrentIndex())) { // object
            maskingState.incrementCurrentIndex(); // step over opening curly bracket
            int objectDepth = 1;
            while (objectDepth > 0) {
                // We need to specifically skip strings to not consider curly brackets which are part of a string
                if (currentByteIsUnescapedDoubleQuote(maskingState)) {
                    // this makes sure that we skip curly brackets (open and close) which are part of strings
                    skipStringValue(maskingState);
                } else {
                    if (AsciiCharacter.isCurlyBracketOpen(maskingState.byteAtCurrentIndex())) {
                        objectDepth++;
                    } else if (AsciiCharacter.isCurlyBracketClose(maskingState.byteAtCurrentIndex())) {
                        objectDepth--;
                    }
                    maskingState.incrementCurrentIndex();
                }
            }
        } else if (AsciiCharacter.isSquareBracketOpen(maskingState.byteAtCurrentIndex())) { // array
            maskingState.incrementCurrentIndex(); // step over opening square bracket
            int arrayDepth = 1;
            while (arrayDepth > 0) {
                // We need to specifically skip strings to not consider square brackets which are part of a string
                if (currentByteIsUnescapedDoubleQuote(maskingState)) {
                    skipStringValue(maskingState);
                } else {
                    if (AsciiCharacter.isSquareBracketOpen(maskingState.byteAtCurrentIndex())) {
                        arrayDepth++;
                    } else if (AsciiCharacter.isSquareBracketClose(maskingState.byteAtCurrentIndex())) {
                        arrayDepth--;
                    }
                    maskingState.incrementCurrentIndex();
                }
            }
        }
    }

    /**
     * This method assumes the masking state is currently at the opening quote of the string value and increments the
     * current index in the masking state until the current index is one position after the string.
     */
    private static void skipStringValue(MaskingState maskingState) {
        maskingState.incrementCurrentIndex(); // step over the opening quote
        while (!currentByteIsUnescapedDoubleQuote(maskingState)) {
            maskingState.incrementCurrentIndex(); // step over the string content
        }
        maskingState.incrementCurrentIndex(); // step over the closing quote
    }
}
