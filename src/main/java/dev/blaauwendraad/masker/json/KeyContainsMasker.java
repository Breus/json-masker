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

        visitValue(maskingState, input, 0, maskingConfig.isInAllowMode() ? maskingConfig.getDefaultConfig() : null);

        return ValueMaskingUtil.flushReplacementOperations(maskingState);
    }

    /**
     * Entrypoint of visiting any value (object, array or primitive) in the JSON.
     *
     * @param maskingState     the current masking state
     * @param keyMaskingConfig if not null it means that the current value is being masked otherwise the value is not
     *                         being masked
     * @return
     */
    private int visitValue(MaskingState maskingState, byte[] input, int index, @CheckForNull KeyMaskingConfig keyMaskingConfig) {
        index = skipWhitespaceCharacters(input, index);
        switch (input[index]) {
            case '-':
            case '+':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '.':
                if (keyMaskingConfig != null && !keyMaskingConfig.isDisableNumberMasking()) {
                    index = maskNumber(maskingState, input, index, keyMaskingConfig);
                } else {
                    index = skipNumericValue(input, index);
                }
                break;
            case '[':
                index = visitArray(maskingState, input, index, keyMaskingConfig);
                break;
            case '{':
                index = visitObject(maskingState, input, index, keyMaskingConfig);
                break;
            case '"':
            case '\'':
                if (keyMaskingConfig != null) {
                    index = maskString(maskingState, input, index, keyMaskingConfig);
                } else {
                    index = skipStringValue(input, index);
                }
                break;
            case 't':
                if (keyMaskingConfig != null && !keyMaskingConfig.isDisableBooleanMasking()) {
                    index = maskBoolean(maskingState, input, index, keyMaskingConfig);
                } else {
                    index += 4;
                }
                break;
            case 'f':
                if (keyMaskingConfig != null && !keyMaskingConfig.isDisableBooleanMasking()) {
                    index = maskBoolean(maskingState, input, index, keyMaskingConfig);
                } else {
                    index += 5;
                }
                break;
            case 'n':
                index += 4;
                break;
        }
        return index;
    }

    /**
     * Visits an array of unknown values (or empty) and invokes {@link #visitValue(MaskingState, KeyMaskingConfig)} on
     * each element while propagating the {@link KeyMaskingConfig}.
     *
     * @param maskingState     the current {@link MaskingState}
     * @param keyMaskingConfig if not null it means that the current value is being masked according to the
     *                         {@link KeyMaskingConfig}. Otherwise, the value is not masked
     * @return
     */
    private int visitArray(MaskingState maskingState, byte[] input, int index, @CheckForNull KeyMaskingConfig keyMaskingConfig) {
        // This block deals with masking arrays
        maskingState.expandCurrentJsonPathWithArray();
        index++; // step over array opening square bracket
        while (input[index] != ']') {
            index = visitValue(maskingState, input, index, keyMaskingConfig);
            index = skipWhitespaceCharacters(input, index);
            if (input[index] == ',') {
                index++;
            }
        }
        index++; // step over array closing square bracket
        maskingState.backtrackCurrentJsonPath();
        return index;
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
     * @return
     */
    private int visitObject(MaskingState maskingState, byte[] input, int index, @CheckForNull KeyMaskingConfig parentKeyMaskingConfig) {
        index++; // step over opening curly bracket
        index = skipWhitespaceCharacters(input, index);
        while (input[index] != '}') {
            // In case target keys should be considered as allow list, we need to NOT mask certain keys
            int openingQuoteIndex = index;
            index++; // step over the JSON key opening quote
            while (input[index] != '"' || input[index - 1] == '\\') {
                index++;
            }

            int closingQuoteIndex = index;
            int keyLength = closingQuoteIndex - openingQuoteIndex - 1; // minus one for the quote
            maskingState.expandCurrentJsonPath(openingQuoteIndex + 1, keyLength);
            KeyMaskingConfig keyMaskingConfig = keyMatcher.getMaskConfigIfMatched(
                    input,
                    openingQuoteIndex + 1, // plus one for the opening quote
                    keyLength,
                    maskingState.getCurrentJsonPath()
            );
            index++;// step over the JSON key closing quote
            index = skipWhitespaceCharacters(input, index);
            index++; // step over the colon ':'
            index = skipWhitespaceCharacters(input, index);

            // if we're in the allow mode, then getting a null as config, means that the key has been explicitly
            // allowed and must not be masked, even if enclosing object is being masked
            boolean valueAllowed = maskingConfig.isInAllowMode() && keyMaskingConfig == null;
            if (valueAllowed) {
                index = skipValue(input, index);
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
                index = visitValue(maskingState, input, index, keyMaskingConfig);
            }
            index = skipWhitespaceCharacters(input, index);
            if (input[index] == ',') {
                index++; // step over comma separating elements
            }
            index = skipWhitespaceCharacters(input, index);

            maskingState.backtrackCurrentJsonPath();
        }
        index++; // step over closing curly bracket
        return index;
    }

    /**
     * Masks the string value in the message of the {@link MaskingState}, starting from the current index which should
     * be at the opening quote of the string value.
     *
     * @param maskingState     the current {@link MaskingState} for which the current index must correspond to the
     *                         opening quote of the string value in the input array
     * @param keyMaskingConfig the {@link KeyMaskingConfig} for the corresponding JSON key
     * @return
     */
    private int maskString(MaskingState maskingState, byte[] input, int index, KeyMaskingConfig keyMaskingConfig) {
        index++; // step over the string value opening quote
        int targetValueLength = 0;
        int noOfEscapeCharacters = 0;
        int additionalBytesForEncoding = 0;
        boolean isEscapeCharacter = false;
        boolean previousCharacterCountedAsEscapeCharacter = false;
        while (!AsciiCharacter.isDoubleQuote(input[index]) || (
                AsciiCharacter.isDoubleQuote(input[index])
                        && isEscapeCharacter)) {
            if (Utf8Util.getCodePointByteLength(input[index]) > 1) {
                /*
                 * We only support UTF-8, so whenever code points are encoded using multiple bytes this should
                 * be represented by a single asterisk and the additional bytes used for encoding need to be
                 * removed.
                 */
                additionalBytesForEncoding += Utf8Util.getCodePointByteLength(input[index]) - 1;
            }
            isEscapeCharacter =
                    AsciiCharacter.isEscapeCharacter(input[index])
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
                        && AsciiCharacter.isLowercaseU(input[index])) {
                    /*
                     * The next 4 characters are hexadecimal digits which form a single character and are only
                     * there for encoding, so must not be included in the mask.
                     */
                    additionalBytesForEncoding += 4;
                }
                previousCharacterCountedAsEscapeCharacter = false;
            }
            targetValueLength++;
            index++;
        }
        if (keyMaskingConfig.getMaskStringsWith() != null) {
            ValueMaskingUtil.replaceTargetValueWith(
                    maskingState,
                    index,
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
                    index,
                    targetValueLength,
                    keyMaskingConfig.getMaskStringCharactersWith(),
                    maskLength
            );
        } else {
            throw new IllegalStateException("Invalid string masking configuration");
        }
        index++; // step over closing quote of string value to start looking for the next JSON key.
        return index;
    }

    /**
     * Masks the numeric value in the message of the {@link MaskingState}, starting from the current index which should
     * be at the first numeric character of numeric value.
     *
     * @param maskingState     the current {@link MaskingState} for which the current index must correspond to the first
     *                         numeric character of the numeric value in the input array
     * @param keyMaskingConfig the {@link KeyMaskingConfig} for the corresponding JSON key
     * @return
     */
    private int maskNumber(MaskingState maskingState, byte[] input, int index, KeyMaskingConfig keyMaskingConfig) {
        // This block deals with numeric values
        int targetValueLength = 0;
        while (index < input.length
                && AsciiJsonUtil.isNumericCharacter(input[index])) {
            targetValueLength++;
            /*
             * Following line cannot result in ArrayOutOfBound because of the early return after checking for
             * first char being a double quote.
             */
            index++;
        }
        if (keyMaskingConfig.getMaskNumbersWith() != null) {
            ValueMaskingUtil.replaceTargetValueWith(
                    maskingState,
                    index,
                    targetValueLength,
                    keyMaskingConfig.getMaskNumbersWith(),
                    1
            );
        } else if (keyMaskingConfig.getMaskNumberDigitsWith() != null) {
            ValueMaskingUtil.replaceTargetValueWith(
                    maskingState,
                    index,
                    targetValueLength,
                    keyMaskingConfig.getMaskNumberDigitsWith(),
                    targetValueLength
            );
        } else {
            throw new IllegalStateException("Invalid number masking configuration");
        }
        return index;
    }

    /**
     * Masks the boolean value in the message of the {@link MaskingState}, starting from the current index which should
     * be at the first character of the boolean value.
     *
     * @param maskingState     the current {@link MaskingState} for which the current index must correspond to the first
     *                         character of the boolean value in the input array
     * @param keyMaskingConfig the {@link KeyMaskingConfig} for the corresponding JSON key
     */
    private int maskBoolean(MaskingState maskingState, byte[] input, int index, KeyMaskingConfig keyMaskingConfig) {
        int targetValueLength = AsciiCharacter.isLowercaseT(input[index]) ? 4 : 5;
        index += targetValueLength;
        if (keyMaskingConfig.getMaskBooleansWith() != null) {
            ValueMaskingUtil.replaceTargetValueWith(
                    maskingState,
                    index,
                    targetValueLength,
                    keyMaskingConfig.getMaskBooleansWith(),
                    1
            );
            return index;
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
    private static int skipValue(byte[] input, int index) {
        index = skipWhitespaceCharacters(input, index);
        switch (input[index]) {
            case '-':
            case '+':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '.':
                index = skipNumericValue(input, index);
                break;
            case '[':
                index = skipArrayValue(input, index);
                break;
            case '{':
                index = skipObjectValue(input, index);
                break;
            case '"':
            case '\'':
                index = skipStringValue(input, index);
                break;
            case 't':
                index += 4;
                break;
            case 'f':
                index += 5;
                break;
            case 'n':
                index += 4;
                break;
        }
        return index;
    }

    /**
     * Checks if the byte at the current index in the {@link MaskingState} is a white space character and if so,
     * increments the index by one. Returns as soon as the byte at the current index in the masking state is not a white
     * space character.
     *
     */
    private static int skipWhitespaceCharacters(byte[] input, int index) {
        while (index < input.length) {
            switch (input[index]) {
                case '\n':
                case '\r':
                case '\t':
                case ' ':
                    index++;
                    break;
                default:
                    return index;
            }
        }
        return index;
    }

    /**
     * This method assumes the masking state is currently at the first numeric character of a numeric value and
     * increments the current index in the masking state until the current index is one position after the numeric
     * value.
     *
     * @return
     */
    private static int skipNumericValue(byte[] input, int index) {
        index++;
        while (index < input.length) {
            switch (input[index]) {
                case '-':
                case '+':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '.':
                case 'e':
                case 'E':
                    index++;
                    break;
                default:
                    return index;
            }
        }
        return index;
    }

    /**
     * This method assumes the masking state is currently at the opening quote of a string value and increments the
     * current index in the masking state until the current index is one position after the string.
     *
     * @return
     */
    private static int skipStringValue(byte[] input, int index) {
        index++;
        boolean escape = false;
        main: while (index < input.length) {
            switch (input[index]) {
                case '\\':
                    escape = true;
                    index++;
                    break;
                case '"':
                    if (!escape) {
                        index++;
                        break main;
                    }
                default:
                    escape = false;
                    index++;
                    break;
            }
        }
        return index;
    }

    /**
     * This method assumes the masking state is currently at the opening curly bracket of an object value and increments
     * the current index in the masking state until the current index is one position after the closing curly bracket of
     * the object.
     */
    private static int skipObjectValue(byte[] input, int index) {
        index++; // step over opening curly bracket
        int objectDepth = 1;
        while (objectDepth > 0) {
            // We need to specifically skip strings to not consider curly brackets which are part of a string
            if (input[index] == '"' && input[index - 1] != '\\') {
                // this makes sure that we skip curly brackets (open and close) which are part of strings
                index = skipStringValue(input, index);
            } else {
                if (input[index] == '{') {
                    objectDepth++;
                } else if (input[index] == '}') {
                    objectDepth--;
                }
                index++;
            }
        }
        return index;
    }

    /**
     * This method assumes the masking state is currently at the opening square bracket of an array value and increments
     * the current index in the masking state until the current index is one position after the closing square bracket
     * of the array.
     *
     * @return
     */
    private static int skipArrayValue(byte[] input, int index) {
        index++; // step over opening square bracket
        int arrayDepth = 1;
        while (arrayDepth > 0) {
            // We need to specifically skip strings to not consider square brackets which are part of a string
            if (input[index] == '"' && input[index - 1] != '\\') {
                index = skipStringValue(input, index);
            } else {
                if (input[index] == '[') {
                    arrayDepth++;
                } else if (input[index] == ']') {
                    arrayDepth--;
                }
                index++;
            }
        }
        return index;
    }
}
