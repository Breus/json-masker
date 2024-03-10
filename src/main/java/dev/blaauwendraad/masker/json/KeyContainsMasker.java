package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;
import dev.blaauwendraad.masker.json.util.AsciiCharacter;
import dev.blaauwendraad.masker.json.util.AsciiJsonUtil;
import dev.blaauwendraad.masker.json.util.Utf8Util;
import dev.blaauwendraad.masker.json.util.ValueMaskingUtil;

import javax.annotation.CheckForNull;

/**
 * Default implementation of the {@link JsonMasker}.
 */
public final class KeyContainsMasker implements JsonMasker {
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
     * @param maskingConfig the {@link JsonMaskingConfig} for the created masker
     */
    public KeyContainsMasker(JsonMaskingConfig maskingConfig) {
        this.maskingConfig = maskingConfig;
        this.keyMatcher = new KeyMatcher(maskingConfig);
    }

    /**
     * Masks the values in the given input for all values having keys corresponding to any of the provided target keys.
     * This implementation is optimized for multiple target keys. Since RFC-8529 dictates that JSON exchanges between
     * systems that are not part of an enclosed system MUST be encoded using UTF-8, this method assumes UTF-8 encoding.
     *
     * @param input the input message for which values might be masked
     * @return the masked message
     */
    @Override
    public byte[] mask(byte[] input) {
        MaskingState maskingState = new MaskingState(input, !maskingConfig.getTargetJsonPaths().isEmpty());

        visitValue(maskingState, maskingConfig.isInAllowMode() ? maskingConfig.getDefaultConfig() : null);

        return ValueMaskingUtil.flushReplacementOperations(maskingState);
    }

    /**
     * Entrypoint of visiting any value (object, array or primitive) in the JSON.
     *
     * @param maskingState     the current masking state
     * @param keyMaskingConfig if not null it means that the current value is being masked otherwise the value is not
     *                         being masked
     */
    private void visitValue(MaskingState maskingState, @CheckForNull KeyMaskingConfig keyMaskingConfig) {
        skipWhitespaceCharacters(maskingState);
        // using switch-case over ifs to improve performance by ~20% (measured in benchmarks)
        switch (maskingState.byteAtCurrentIndex()) {
            case '[' -> visitArray(maskingState, keyMaskingConfig);
            case '{' -> visitObject(maskingState, keyMaskingConfig);
            case '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                if (keyMaskingConfig != null && !keyMaskingConfig.isDisableNumberMasking()) {
                    maskNumber(maskingState, keyMaskingConfig);
                } else {
                    skipNumericValue(maskingState);
                }
            }
            case '"' -> {
                if (keyMaskingConfig != null) {
                    maskString(maskingState, keyMaskingConfig);
                } else {
                    skipStringValue(maskingState);
                }
            }
            case 't' -> {
                if (keyMaskingConfig != null && !keyMaskingConfig.isDisableBooleanMasking()) {
                    maskBoolean(maskingState, keyMaskingConfig);
                } else {
                    maskingState.setCurrentIndex(maskingState.currentIndex() + 4);
                }
            }
            case 'f' -> {
                if (keyMaskingConfig != null && !keyMaskingConfig.isDisableBooleanMasking()) {
                    maskBoolean(maskingState, keyMaskingConfig);
                } else {
                    maskingState.setCurrentIndex(maskingState.currentIndex() + 5);
                }
            }
            case 'n' -> maskingState.setCurrentIndex(maskingState.currentIndex() + 4);
        }
    }

    /**
     * Visits an array of unknown values (or empty) and invokes {@link #visitValue(MaskingState, KeyMaskingConfig)} on
     * each element while propagating the {@link KeyMaskingConfig}.
     *
     * @param maskingState     the current {@link MaskingState}
     * @param keyMaskingConfig if not null it means that the current value is being masked according to the
     *                         {@link KeyMaskingConfig}. Otherwise, the value is not masked
     */
    private void visitArray(MaskingState maskingState, @CheckForNull KeyMaskingConfig keyMaskingConfig) {
        // This block deals with masking arrays
        maskingState.expandCurrentJsonPathWithArray();
        maskingState.incrementCurrentIndex(); // step over array opening square bracket
        while (!AsciiCharacter.isSquareBracketClose(maskingState.byteAtCurrentIndex())) {
            visitValue(maskingState, keyMaskingConfig);
            skipWhitespaceCharacters(maskingState);
            if (AsciiCharacter.isComma(maskingState.byteAtCurrentIndex())) {
                maskingState.incrementCurrentIndex();
            }
        }
        maskingState.incrementCurrentIndex(); // step over array closing square bracket
        maskingState.backtrackCurrentJsonPath();
    }

    /**
     * Visits an object, iterates over the keys and checks whether key needs to be masked (if
     * {@link JsonMaskingConfig.TargetKeyMode#MASK}) or allowed (if {@link JsonMaskingConfig.TargetKeyMode#ALLOW}). For
     * each value, invokes {@link #visitValue(MaskingState, KeyMaskingConfig)} with a non-null {@link KeyMaskingConfig}
     * (when key needs to be masked) or {@code null} (when key is allowed). Whenever 'parentKeyMaskingConfig' is
     * supplied, it means that the object with all its keys is being masked. The only situation when the individual
     * values do not need to be masked is when the key is explicitly allowed (in allow mode).
     *
     * @param maskingState           the current {@link MaskingState}
     * @param parentKeyMaskingConfig if not null it means that the current value is being masked according to the
     *                               {@link KeyMaskingConfig}. Otherwise, the value is not being masked
     */
    private void visitObject(MaskingState maskingState, @CheckForNull KeyMaskingConfig parentKeyMaskingConfig) {
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
                skipValue(maskingState);
            } else {
                // this is where it might get confusing - this method is called when the whole object is being masked
                // if we got a maskingConfig for the key - we need to mask this key with that config, but if the config
                // we got was the default config, then it means that the key doesn't have a specific configuration and
                // we should fallback to key specific config, that the object is being masked with
                // e.g. '{ "a": { "b": "value" } }' we want to use config of 'b' if any, but fallback to config of 'a'
                if (parentKeyMaskingConfig != null && (keyMaskingConfig == null
                        || keyMaskingConfig == maskingConfig.getDefaultConfig())) {
                    keyMaskingConfig = parentKeyMaskingConfig;
                }
                visitValue(maskingState, keyMaskingConfig);
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

    /**
     * Masks the string value in the message of the {@link MaskingState}, starting from the current index which should
     * be at the opening quote of the string value.
     *
     * @param maskingState     the current {@link MaskingState} for which the current index must correspond to the
     *                         opening quote of the string value in the input array
     * @param keyMaskingConfig the {@link KeyMaskingConfig} for the corresponding JSON key
     */
    private void maskString(MaskingState maskingState, KeyMaskingConfig keyMaskingConfig) {
        maskingState.incrementCurrentIndex(); // step over the string value opening quote
        int targetValueLength = 0;
        int noOfEscapeCharacters = 0;
        int additionalBytesForEncoding = 0;
        boolean isEscapeCharacter = false;
        boolean previousCharacterCountedAsEscapeCharacter = false;
        while (!AsciiCharacter.isDoubleQuote(maskingState.byteAtCurrentIndex()) || (
                AsciiCharacter.isDoubleQuote(maskingState.byteAtCurrentIndex())
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
                    AsciiCharacter.isEscapeCharacter(maskingState.byteAtCurrentIndex())
                            && !previousCharacterCountedAsEscapeCharacter;
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
     * Masks the numeric value in the message of the {@link MaskingState}, starting from the current index which should
     * be at the first numeric character of numeric value.
     *
     * @param maskingState     the current {@link MaskingState} for which the current index must correspond to the first
     *                         numeric character of the numeric value in the input array
     * @param keyMaskingConfig the {@link KeyMaskingConfig} for the corresponding JSON key
     */
    private void maskNumber(MaskingState maskingState, KeyMaskingConfig keyMaskingConfig) {
        // This block deals with numeric values
        int targetValueLength = 0;
        while (maskingState.currentIndex() < maskingState.getMessage().length
                && AsciiJsonUtil.isNumericCharacter(maskingState.byteAtCurrentIndex())) {
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
     * Masks the boolean value in the message of the {@link MaskingState}, starting from the current index which should
     * be at the first character of the boolean value.
     *
     * @param maskingState     the current {@link MaskingState} for which the current index must correspond to the first
     *                         character of the boolean value in the input array
     * @param keyMaskingConfig the {@link KeyMaskingConfig} for the corresponding JSON key
     */
    private void maskBoolean(MaskingState maskingState, KeyMaskingConfig keyMaskingConfig) {
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
     * This method assumes the masking state is currently at the first byte of a JSON value which can be any of: array,
     * boolean, object, null, number, or string and increments the current index in the masking state until the current
     * index is one position after the value.
     * <p>
     * Note: in case the value is an object or array, it skips the entire object and array and all the elements it
     * includes (e.g. nested arrays, objects, etc.).
     */
    private static void skipValue(MaskingState maskingState) {
        switch (maskingState.byteAtCurrentIndex()) {
            case '"' -> skipStringValue(maskingState);
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-' -> skipNumericValue(maskingState);
            case 't', 'n' -> maskingState.setCurrentIndex(maskingState.currentIndex() + 4);
            case 'f' -> maskingState.setCurrentIndex(maskingState.currentIndex() + 5);
            case '{' -> skipObjectValue(maskingState);
            case '[' -> skipArrayValue(maskingState);
        }
    }

    /**
     * Checks if the byte at the current index in the {@link MaskingState} is a white space character and if so,
     * increments the index by one. Returns as soon as the byte at the current index in the masking state is not a white
     * space character.
     *
     * @param maskingState the current {@link MaskingState}
     */
    private static void skipWhitespaceCharacters(MaskingState maskingState) {
        while (AsciiJsonUtil.isWhiteSpace(maskingState.byteAtCurrentIndex())) {
            maskingState.incrementCurrentIndex();
        }
    }

    /**
     * This method assumes the masking state is currently at the first numeric character of a numeric value and
     * increments the current index in the masking state until the current index is one position after the numeric
     * value.
     */
    private static void skipNumericValue(MaskingState maskingState) {
        maskingState.incrementCurrentIndex(); // step over the first numeric character
        while (maskingState.currentIndex() < maskingState.getMessage().length
                && AsciiJsonUtil.isNumericCharacter(maskingState.byteAtCurrentIndex())) {
            maskingState.incrementCurrentIndex();
        }
    }

    /**
     * This method assumes the masking state is currently at the opening quote of a string value and increments the
     * current index in the masking state until the current index is one position after the string.
     */
    private static void skipStringValue(MaskingState maskingState) {
        maskingState.incrementCurrentIndex(); // step over the opening quote
        while (!currentByteIsUnescapedDoubleQuote(maskingState)) {
            maskingState.incrementCurrentIndex(); // step over the string content
        }
        maskingState.incrementCurrentIndex(); // step over the closing quote
    }

    /**
     * This method assumes the masking state is currently at the opening curly bracket of an object value and increments
     * the current index in the masking state until the current index is one position after the closing curly bracket of
     * the object.
     */
    private static void skipObjectValue(MaskingState maskingState) {
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
    }

    /**
     * This method assumes the masking state is currently at the opening square bracket of an array value and increments
     * the current index in the masking state until the current index is one position after the closing square bracket
     * of the array.
     */
    private static void skipArrayValue(MaskingState maskingState) {
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

    /**
     * Checks if the byte at the current index in the {@link MaskingState} is an unescaped double quote character in
     * UTF-8.
     *
     * @param maskingState the current {@link MaskingState}
     * @return whether the byte at the current index is an unescaped double quote
     */
    private static boolean currentByteIsUnescapedDoubleQuote(MaskingState maskingState) {
        return AsciiCharacter.isDoubleQuote(maskingState.byteAtCurrentIndex())
                && !AsciiCharacter.isEscapeCharacter(maskingState.byteAtCurrentIndexMinusOne());
    }
}
