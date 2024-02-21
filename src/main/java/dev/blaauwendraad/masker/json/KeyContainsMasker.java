package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;
import dev.blaauwendraad.masker.json.util.AsciiCharacter;
import dev.blaauwendraad.masker.json.util.AsciiJsonUtil;
import dev.blaauwendraad.masker.json.util.Utf8Util;
import dev.blaauwendraad.masker.json.util.ValueMaskingUtil;

import static dev.blaauwendraad.masker.json.util.AsciiCharacter.isComma;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.isCurlyBracketClose;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.isEscapeCharacter;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.isSquareBracketClose;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.isSquareBracketOpen;

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
    private final KeyMatcher keyMatcher;
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
        this.keyMatcher = new KeyMatcher(maskingConfig);
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
        MaskingState maskingState = new MaskingState(input, 1, !maskingConfig.getTargetJsonPaths().isEmpty());
        if (AsciiCharacter.isSquareBracketOpen(maskingState.byteAtCurrentIndexMinusOne())) {
            maskingState.expandCurrentJsonPath();
        }
        mainLoop:
        while (maskingState.currentIndex() < maskingState.messageLength() - MIN_OFFSET_JSON_KEY_QUOTE) {
            // Find JSON strings by looking for unescaped double quotes
            while (!currentByteIsUnescapedDoubleQuote(maskingState)) {
                trackCurrentJsonPath(maskingState);
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
            int keyLength = closingQuoteIndex - openingQuoteIndex - 1; // minus one for the quote
            maskingState.incrementCurrentIndex(); //  The current index is at the colon between the key and value, step over the colon.
            skipWhitespaceCharacters(maskingState); // Step over all white characters after the colon,
            // Depending on the masking configuration, Strings, Numbers, Arrays and/or Objects should be masked.

            /*
             * At this point, we found a JSON key with a maskable value, which is either a string, number, array,
             * or object. Now let's verify the found JSON key is a target key.
             */
            maskingState.expandCurrentJsonPath(openingQuoteIndex + 1, keyLength);
            KeyMaskingConfig keyMaskingConfig = keyMatcher.getMaskConfigIfMatched(
                    maskingState.getMessage(),
                    openingQuoteIndex + 1, // plus one for the opening quote
                    keyLength,
                    maskingState.getCurrentJsonPath()
            );

            // null means key should not be masked
            if (keyMaskingConfig == null) {
                if (maskingConfig.isInAllowMode()) {
                    // the value belongs to a JSON key which is explicitly allowed, so skip it, even if that's object
                    skipAllValues(maskingState);
                    maskingState.backtrackCurrentJsonPath();
                    continue;
                } else {
                    // Check if the current key does not open a new json object or json array
                    if (!(AsciiCharacter.isCurlyBracketOpen(maskingState.byteAtCurrentIndex())
                            || AsciiCharacter.isSquareBracketOpen(maskingState.byteAtCurrentIndex()))) {
                        maskingState.backtrackCurrentJsonPath();
                    }
                    // The found JSON key is not a target key, so continue looking from where we left of.
                    continue;
                }
            }

            // Masking value based on type
            if (AsciiCharacter.isSquareBracketOpen(maskingState.byteAtCurrentIndex())) {
                maskArrayValueInPlace(maskingState, keyMaskingConfig);
            } else if (AsciiCharacter.isCurlyBracketOpen(maskingState.byteAtCurrentIndex())) {
                maskObjectValueInPlace(maskingState, keyMaskingConfig);
            } else if (AsciiCharacter.isDoubleQuote(maskingState.byteAtCurrentIndex())) {
                // This block deals with masking strings target values.
                maskStringValueInPlace(maskingState, keyMaskingConfig);
            } else if (AsciiJsonUtil.isFirstNumberChar(maskingState.byteAtCurrentIndex())
                    && !keyMaskingConfig.isDisableNumberMasking()) {
                maskNumberValueInPlace(maskingState, keyMaskingConfig);
            } else if ((AsciiCharacter.isLowercaseF(maskingState.byteAtCurrentIndex())
                    || AsciiCharacter.isLowercaseT(maskingState.byteAtCurrentIndex()))
                    && !keyMaskingConfig.isDisableBooleanMasking()) {
                maskBooleanValueInPlace(maskingState, keyMaskingConfig);
            }
            // else, if nothing has matched, continue to the next value
            // it may happen that we don't mask anything if number / boolean masking is disabled or the value is null

            maskingState.backtrackCurrentJsonPath();
        }

        ValueMaskingUtil.flushReplacementOperations(maskingState);

        return maskingState.getMessage();
    }

    /**
     * Checks if the current byte changes the current json path. If so, updates current json path.
     */
    private void trackCurrentJsonPath(MaskingState maskingState) {
        // Check if this is the end of the current json object
        if (!maskingState.isInArray() && currentByteIsUnescapedCurlyBracketClose(maskingState)) {
            maskingState.backtrackCurrentJsonPath();
        }
        // Check if this is the end of the current json array
        if (maskingState.isInArray() && currentByteIsUnescapedSquareBracketClose(maskingState)) {
            maskingState.backtrackCurrentJsonPath();
            // if it is not a nested array, backtrack the parent key component as well
            if (!maskingState.isInArray()) {
                maskingState.backtrackCurrentJsonPath();
            }
        }
        // Check if this is the end of the current json array element
        if (maskingState.isInArray() && currentByteIsUnescapedComma(maskingState)) {
            maskingState.incrementCurrentJsonPathArrayIndex();
        }
        // Check if this is the start of a json array
        if (currentByteIsUnescapedSquareBracketOpen(maskingState)) {
            maskingState.expandCurrentJsonPath();
        }
    }

    /**
     * Checks if the byte at the given index in the input byte array is an unescaped double quote character in UTF-8.
     *
     * @param maskingState the current masking state
     * @return whether the byte at index is an unescaped double quote
     */
    private static boolean currentByteIsUnescapedDoubleQuote(MaskingState maskingState) {
        return AsciiCharacter.isDoubleQuote(maskingState.byteAtCurrentIndex())
                && !AsciiCharacter.isEscapeCharacter(maskingState.byteAtCurrentIndexMinusOne());
    }

    private static boolean currentByteIsUnescapedCurlyBracketClose(MaskingState maskingState) {
        return isCurlyBracketClose(maskingState.byteAtCurrentIndex())
                && !isEscapeCharacter(maskingState.byteAtCurrentIndexMinusOne());
    }

    private static boolean currentByteIsUnescapedSquareBracketClose(MaskingState maskingState) {
        return (isSquareBracketClose(maskingState.byteAtCurrentIndex()))
                && !isEscapeCharacter(maskingState.byteAtCurrentIndexMinusOne());
    }

    private static boolean currentByteIsUnescapedSquareBracketOpen(MaskingState maskingState) {
        return (isSquareBracketOpen(maskingState.byteAtCurrentIndex()))
                && !isEscapeCharacter(maskingState.byteAtCurrentIndexMinusOne());
    }

    private static boolean currentByteIsUnescapedComma(MaskingState maskingState) {
        return isComma(maskingState.byteAtCurrentIndex())
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
     * Masks the string value in the provided input while starting from the provided current index which should be at
     * the opening quote of the string value.
     *
     * @param maskingState     the current masking state where for which the current index must correspond to the opening
     *                         quote of the string value in the input array of the current index
     * @param keyMaskingConfig the masking configuration for the key
     */
    private void maskStringValueInPlace(MaskingState maskingState, KeyMaskingConfig keyMaskingConfig) {
        maskingState.incrementCurrentIndex(); // step over the string value opening quote
        int targetValueLength = 0;
        int noOfEscapeCharacters = 0;
        int additionalBytesForEncoding = 0;
        boolean isEscapeCharacter = false;
        boolean previousCharacterCountedAsEscapeCharacter = false;
        while (!AsciiCharacter.isDoubleQuote(maskingState.byteAtCurrentIndex()) || (AsciiCharacter.isDoubleQuote(maskingState.byteAtCurrentIndex())
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
                    AsciiCharacter.isEscapeCharacter(maskingState.byteAtCurrentIndex()) && !previousCharacterCountedAsEscapeCharacter;
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
            targetValueLength++;
            maskingState.incrementCurrentIndex();
        }
        if (keyMaskingConfig.getMaskStringsWith() != null) {
            ValueMaskingUtil.replaceTargetValueWith(
                    maskingState,
                    targetValueLength,
                    keyMaskingConfig.getMaskStringsWith(),
                    1
            );
        } else if (keyMaskingConfig.getMaskStringCharactersWith() != null) {
            /*
            So we don't add asterisks for escape characters or additional encoding bytes (which are not part of the String length)

            The actual length of the string is the length minus escape characters (which are not part of the
            string length). Also, unicode characters are denoted as 4-hex digits but represent actually
            just one character, so for each of them 3 asterisks should be removed.
             */
            int maskLength = targetValueLength - noOfEscapeCharacters - additionalBytesForEncoding;

            ValueMaskingUtil.replaceTargetValueWith(
                    maskingState,
                    targetValueLength,
                    keyMaskingConfig.getMaskStringCharactersWith(),
                    maskLength
            );
        } else {
            throw new IllegalStateException("Invalid string masking configuration");
        }
        maskingState.incrementCurrentIndex(); // step over closing quote of string value to start looking for the next JSON key.
    }

    /**
     * Masks the array of the masking state where the current index is on the opening square bracket
     *
     * @param maskingState     the current masking state in which the array will be masked
     * @param keyMaskingConfig the masking configuration for the key
     */
    private void maskArrayValueInPlace(MaskingState maskingState, KeyMaskingConfig keyMaskingConfig) {
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
                maskStringValueInPlace(maskingState, keyMaskingConfig); // mask string and step over it
            } else if (AsciiJsonUtil.isFirstNumberChar(maskingState.byteAtCurrentIndex())
                    && !keyMaskingConfig.isDisableNumberMasking()) {
                maskNumberValueInPlace(maskingState, keyMaskingConfig);
            } else if (AsciiCharacter.isCurlyBracketOpen(maskingState.byteAtCurrentIndex())) {
                maskObjectValueInPlace(maskingState, keyMaskingConfig);
            } else if ((AsciiCharacter.isLowercaseF(maskingState.byteAtCurrentIndex())
                    || AsciiCharacter.isLowercaseT(maskingState.byteAtCurrentIndex()))
                    && !keyMaskingConfig.isDisableBooleanMasking()) {
                maskBooleanValueInPlace(maskingState, keyMaskingConfig);
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
     * @param maskingState           the current masking state
     * @param parentKeyMaskingConfig the masking configuration for the (parent) key, is used as default if
     *                               the keys of masked object itself do not have a masking configuration
     */
    private void maskObjectValueInPlace(MaskingState maskingState, KeyMaskingConfig parentKeyMaskingConfig) {
        maskingState.incrementCurrentIndex(); // step over opening curly bracket
        skipWhitespaceCharacters(maskingState);
        while (!AsciiCharacter.isCurlyBracketClose(maskingState.byteAtCurrentIndex())) {
            // In case target keys should be considered as allow list, we need to NOT mask certain keys
            int openingQuoteIndex = maskingState.currentIndex();
            maskingState.incrementCurrentIndex(); // step over the JSON key opening quote
            while (!currentByteIsUnescapedDoubleQuote(maskingState)) {
                maskingState.incrementCurrentIndex();
            }

            int closingQuoteIndex = maskingState.currentIndex();
            int keyLength = closingQuoteIndex - openingQuoteIndex - 1; // minus one for the quote
            maskingState.expandCurrentJsonPath(openingQuoteIndex + 1, keyLength);
            KeyMaskingConfig keyMaskingConfig = keyMatcher.getMaskConfigIfMatched(
                    maskingState.getMessage(),
                    openingQuoteIndex + 1, // plus one for the opening quote
                    keyLength,
                    maskingState.getCurrentJsonPath()
            );
            maskingState.incrementCurrentIndex();// step over the JSON key closing quote
            skipWhitespaceCharacters(maskingState);
            maskingState.incrementCurrentIndex(); // step over the colon ':'
            skipWhitespaceCharacters(maskingState);

            // if we're in the allow mode, then getting a null as config, means that the key has been explicitly
            // allowed and must not be masked, even if enclosing object is being masked
            boolean valueAllowed = maskingConfig.isInAllowMode() && keyMaskingConfig == null;
            if (valueAllowed) {
                skipAllValues(maskingState);
            } else {
                // this is where it might get confusing - this method is called when the whole object is being masked
                // if we got a maskingConfig for the key - we need to mask this key with that config, but if the config
                // we got was the default config, then it means that the key doesn't have a specific configuration and
                // we should fallback to key specific config, that the object is being masked with
                // e.g. '{ "a": { "b": "value" } }' we want to use config of 'b' if any, but fallback to config of 'a'
                if (keyMaskingConfig == null || keyMaskingConfig == maskingConfig.getDefaultConfig()) {
                    keyMaskingConfig = parentKeyMaskingConfig;
                }
                if (AsciiCharacter.isSquareBracketOpen(maskingState.byteAtCurrentIndex())) {
                    maskArrayValueInPlace(maskingState, keyMaskingConfig);
                } else if (AsciiCharacter.isDoubleQuote(maskingState.byteAtCurrentIndex())) {
                    maskStringValueInPlace(maskingState, keyMaskingConfig); // mask string and step over it
                } else if (AsciiJsonUtil.isFirstNumberChar(maskingState.byteAtCurrentIndex())
                        && !keyMaskingConfig.isDisableNumberMasking()) {
                    maskNumberValueInPlace(maskingState, keyMaskingConfig);
                } else if (AsciiCharacter.isCurlyBracketOpen(maskingState.byteAtCurrentIndex())) {
                    maskObjectValueInPlace(maskingState, keyMaskingConfig);
                } else if ((AsciiCharacter.isLowercaseF(maskingState.byteAtCurrentIndex())
                        || AsciiCharacter.isLowercaseT(maskingState.byteAtCurrentIndex()))
                        && !keyMaskingConfig.isDisableBooleanMasking()) {
                    maskBooleanValueInPlace(maskingState, keyMaskingConfig);
                } else {
                    while (!AsciiCharacter.isComma(maskingState.byteAtCurrentIndex())
                            && !AsciiCharacter.isCurlyBracketClose(
                            maskingState.byteAtCurrentIndex())) {
                        maskingState.incrementCurrentIndex(); // skip non-maskable value
                    }
                }
            }
            skipWhitespaceCharacters(maskingState);
            if (AsciiCharacter.isComma(maskingState.byteAtCurrentIndex())) {
                maskingState.incrementCurrentIndex(); // step over comma separating elements
            }
            skipWhitespaceCharacters(maskingState);

            maskingState.backtrackCurrentJsonPath();
        }
        maskingState.incrementCurrentIndex(); // step over closing curly bracket
    }

    private void maskNumberValueInPlace(MaskingState maskingState, KeyMaskingConfig keyMaskingConfig) {
        // This block deals with numeric values
        int targetValueLength = 0;
        while (AsciiJsonUtil.isNumericCharacter(maskingState.byteAtCurrentIndex())) {
            targetValueLength++;
            /*
             * Following line cannot result in ArrayOutOfBound because of the early return after checking for
             * first char being a double quote.
             */
            maskingState.incrementCurrentIndex();
        }
        if (keyMaskingConfig.getMaskNumbersWith() != null) {
            ValueMaskingUtil.replaceTargetValueWith(
                    maskingState,
                    targetValueLength,
                    keyMaskingConfig.getMaskNumbersWith(),
                    1
            );
        } else if (keyMaskingConfig.getMaskNumberDigitsWith() != null) {
            ValueMaskingUtil.replaceTargetValueWith(
                    maskingState,
                    targetValueLength,
                    keyMaskingConfig.getMaskNumberDigitsWith(),
                    targetValueLength
            );
        } else {
            throw new IllegalStateException("Invalid number masking configuration");
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

    private void maskBooleanValueInPlace(MaskingState maskingState, KeyMaskingConfig keyMaskingConfig) {
        int targetValueLength = AsciiCharacter.isLowercaseT(maskingState.byteAtCurrentIndex()) ? 4 : 5;
        maskingState.setCurrentIndex(maskingState.currentIndex() + targetValueLength);
        if (keyMaskingConfig.getMaskBooleansWith() != null) {
            ValueMaskingUtil.replaceTargetValueWith(
                    maskingState,
                    targetValueLength,
                    keyMaskingConfig.getMaskBooleansWith(),
                    1
            );
        } else {
            throw new IllegalStateException("Invalid boolean masking configuration");
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
