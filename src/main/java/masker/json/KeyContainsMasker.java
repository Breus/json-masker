package masker.json;

import masker.AsciiCharacter;
import masker.Utf8Util;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static masker.AsciiCharacter.isDoubleQuote;
import static masker.AsciiCharacter.isEscapeCharacter;
import static masker.json.AsciiJsonUtil.isFirstNumberChar;
import static masker.json.AsciiJsonUtil.isNumericCharacter;
import static masker.json.AsciiJsonUtil.isWhiteSpace;

public final class KeyContainsMasker implements JsonMasker {
    /*
     * We are looking for targeted JSON keys, so the closing quote can appear at minimum 4 characters till the end of
     *  the JSON in the following minimal case: '{"":""}'
     */
    private static final int MIN_OFFSET_JSON_KEY_QUOTE = 4;
    /*
     * Minimum JSON for which masking could be required is: {"":""}, so minimum length at least 7 bytes.
     */
    private static final int MIN_MASKABLE_JSON_LENGTH = 7;
    private final Set<String> targetKeys;


    /**
     * 1. mask all keys corresponding to key (done)
     * 2. maks a key only top-level ($.key)
     * 3. mask a key in some object path (object.inner.key)
     *
     *
     * {
     *     "key" {
     *         "key1": "secret",
     *         "key2": "secret"
     *         "Key3": {
     *             "key4": ""
     *             "key2": ""
     *         }
     *     }
     * }
     *
     *
     *    "key2"
     *  "obj.secret"
     *  "obj.otherSecret"
     * key.key1
     * key.key3
     * key.**
     *
     * class MyData {
     *     private String ssn;
     *     private String secret;
     *     private InnerObj obj;
     * }
     *
     * class InnerObj {
     *     @MaskMe
     *     private String secret;
     *     @MaskMe
     *     private String otherSecret;
     * }
     *
     */
    // /settings/somefield/
    // $.phoneNumbers[:1].type

    private final JsonMaskingConfig maskingConfig;

    public KeyContainsMasker(JsonMaskingConfig maskingConfig) {
        this.targetKeys = maskingConfig.getTargetKeys();
        this.maskingConfig = maskingConfig;
    }

    /**
     * Masks the values in the given input for all values having keys corresponding to any of the provided target keys.
     * This implementation is optimized for multiple target keys.
     * Currently, only supports UTF_8 character encoding
     *
     * @param input the input message for which values might be masked
     * @return the masked message
     */
    @Override
    public byte[] mask(byte[] input) {
        /*
         * No masking required if input is not an JSON array or JSON object (starting with either '{' or
         * '['), or input is shorter than the minimal maskable JSON input.
         */
        if (!isObjectOrArray(input) || input.length < MIN_MASKABLE_JSON_LENGTH) {
            return input;
        }
        /*
         * We can start the index at 1 since the first character can be skipped as it is either a '{' or '[', ensures
         * we can safely check for unescaped double quotes (without masking JSON string values).
         */
        int i = 1;
        mainLoop:
        while (i < input.length - MIN_OFFSET_JSON_KEY_QUOTE) {
            // Find JSON strings by looking for unescaped double quotes
            while (!isUnescapedDoubleQuote(i, input)) {
                if (i < input.length - MIN_OFFSET_JSON_KEY_QUOTE - 1) {
                    i++;
                } else {
                    break mainLoop;
                }
            }
            int openingQuoteIndex = i;
            i++; // step over the opening quote
            while (!isUnescapedDoubleQuote(i, input) && i < input.length - 1) {
                if (i < input.length - MIN_OFFSET_JSON_KEY_QUOTE) {
                    i++;
                } else {
                    break mainLoop;
                }
            }
            int closingQuoteIndex = i;
            i++; // Step over the closing quote.

            /*
             * At this point, we found a JSON string ("...").
             * Now let's verify it is a JSON key (it must be followed by a colon with some white spaces between
             * the string value and the colon).
             */
            while (!AsciiCharacter.isColon(input[i])) {
                if (!AsciiJsonUtil.isWhiteSpace(input[i])) {
                    continue mainLoop; // The found string was not a JSON key, continue looking from where we left of.
                }
                i++;
            }

            /*
             * At this point, we found a string which is in fact a JSON key.
             * Now let's verify that the value is maskable (a number or string).
             */
            i++; //  The index is at the colon between the key and value, step over the colon.
            while (isWhiteSpace(input[i])) {
                i++; // Step over all white characters after the colon,
            }
            if (!isDoubleQuote(input[i])) {
                if (maskingConfig.isNumberMaskingDisabled()) {
                    // The found JSON key did not have a maskable value, continue looking from where we left of.
                    continue mainLoop;
                } else {
                    if (!isFirstNumberChar(input[i])) {
                        // The found JSON key did not have a maskable value, continue looking from where we left of.
                        continue mainLoop;
                    }
                }
            }

            /*
             * At this point, we found a JSON key with a maskable value.
             * Now let's verify the found JSON key is a target key.
             */
            int keyLength = closingQuoteIndex - openingQuoteIndex - 1; // minus one for the quote
            byte[] keyBytesBuffer = new byte[keyLength];
            System.arraycopy(input, openingQuoteIndex + 1, keyBytesBuffer, 0, keyLength);
            String key = new String(keyBytesBuffer, StandardCharsets.UTF_8);
            if (!targetKeys.contains(key)) {
                continue mainLoop; // The found JSON key is not a target key, so continue the main loop
            }

            /*
             * At this point, we found a targeted JSON key with a maskable value.
             * Now let's mask the value.
             */
            int obfuscationLength = maskingConfig.getObfuscationLength();
            if (maskingConfig.isNumberMaskingEnabled() && isNumericCharacter(input[i])) {
                // This block deals with numeric values
                int targetValueLength = 0;
                while (isNumericCharacter(input[i])) {
                    targetValueLength++;
                    input[i] = AsciiCharacter.toAsciiByteValue(maskingConfig.getMaskNumberValuesWith());
                    /*
                     * Following line cannot result in ArrayOutOfBound because of the early return after checking for
                     * first char being a double quote.
                     */
                    i++;
                }
                if (maskingConfig.isObfuscationEnabled() && obfuscationLength != targetValueLength) {
                    if (obfuscationLength == 0) {
                        /*
                         * For obfuscation length 0, we want to obfuscate numeric values with a single 0 because an
                         * empty numeric value is illegal JSON.
                         */
                        input = FixedLengthTargetValueMaskUtil.replaceTargetValueWithFixedLengthMask(
                                input,
                                i,
                                1,
                                targetValueLength,
                                AsciiCharacter.toAsciiByteValue(maskingConfig.getMaskNumberValuesWith())
                        );
                        /*
                         * The length of the input got changed, so we need to compensate that on our index by
                         * stepping the difference between value length and obfuscation length back.
                         */
                        i = i - (targetValueLength - 1);
                    } else {
                        input = FixedLengthTargetValueMaskUtil.replaceTargetValueWithFixedLengthMask(
                                input,
                                i,
                                obfuscationLength,
                                targetValueLength,
                                AsciiCharacter.toAsciiByteValue(maskingConfig.getMaskNumberValuesWith())
                        );
                        /*
                         * The length of the input got changed, so we need to compensate that on our index by
                         * stepping the difference between value length and obfuscation length back.
                         */
                        i = i - (targetValueLength - obfuscationLength);
                    }
                }
            } else {
                // This block deals with masking strings target values.
                i++; // step over quote
                int targetValueLength = 0;
                int noOfEscapeCharacters = 0;
                int additionalBytesForEncoding = 0;
                boolean escapeNextCharacter = false;
                boolean previousCharacterCountedAsEscapeCharacter = false;
                while (!isDoubleQuote(input[i]) || (isDoubleQuote(input[i]) && escapeNextCharacter)) {
                    if (Utf8Util.getCodePointByteLength(input[i]) > 1) {
                        /*
                         * We only support UTF-8, so whenever code points are encoded using multiple bytes this should
                         * be represented by a single asterisk and the additional bytes used for encoding need to be
                         * removed.
                         */
                        additionalBytesForEncoding += Utf8Util.getCodePointByteLength(input[i]) - 1;
                    }
                    escapeNextCharacter = isEscapeCharacter(input[i]);
                    if (escapeNextCharacter && !previousCharacterCountedAsEscapeCharacter) {
                        /*
                         * Non-escaped backslashes are escape characters and are not actually part of the string but
                         * only used for character encoding, so must not be included in the mask.
                         */
                        noOfEscapeCharacters++;
                        previousCharacterCountedAsEscapeCharacter = true;
                    } else {
                        if (previousCharacterCountedAsEscapeCharacter && AsciiCharacter.isLowercaseU(input[i])) {
                            /*
                             * The next 4 characters are hexadecimal digits which form a single character and are only
                             * there for encoding, so must not be included in the mask.
                             */
                            additionalBytesForEncoding += 4;
                        }
                        previousCharacterCountedAsEscapeCharacter = false;
                    }
                    input[i] = AsciiCharacter.ASTERISK.getAsciiByteValue();
                    targetValueLength++;
                    i++;
                }
                if (maskingConfig.isObfuscationEnabled()
                        && obfuscationLength != (targetValueLength - noOfEscapeCharacters)) {
                    input = FixedLengthTargetValueMaskUtil.replaceTargetValueWithFixedLengthAsteriskMask(
                            input,
                            i,
                            obfuscationLength,
                            targetValueLength
                    );
                    /*
                     * Compensate the index for shortening the input by setting fixed length to be at the closing
                     * double quote of the string value.
                     */
                    i = i - (targetValueLength - obfuscationLength);
                } else if (noOfEscapeCharacters > 0 || additionalBytesForEncoding > 0) {
                    // So we don't add asterisks for escape characters or additional encoding bytes (which
                    // are not part of the String length)

                    /*
                     * The actual length of the string is the length minus escape characters (which are not part of the
                     * string length). Also, unicode characters are denoted as 4-hex digits but represent actually
                     * just one character, so for each of them 3 asterisks should be removed.
                     */
                    int actualStringLength = targetValueLength - noOfEscapeCharacters - additionalBytesForEncoding;
                    input = FixedLengthTargetValueMaskUtil.replaceTargetValueWithFixedLengthAsteriskMask(
                            input,
                            i,
                            actualStringLength,
                            targetValueLength
                    );
                    /*
                     * Compensate the index for shortening the input with noOfEscapeCharacters to be at closing
                     * double quote of the String value.
                     */
                    i = i - noOfEscapeCharacters - additionalBytesForEncoding;
                }
                i++; // step over closing quote of string value to start looking for the next JSON key.
            }
        }
        return input;
    }

    /**
     * Checks if the byte at the given index in the input byte array is an unescaped double quote character in UTF-8.
     *
     * @param i     the index, must be >= 1
     * @param input the input byte array
     * @return whether the byte at index i is an unescaped double quote
     */
    private boolean isUnescapedDoubleQuote(int i, byte[] input) {
        return isDoubleQuote(input[i]) && !isEscapeCharacter(input[i - 1]);
    }

    private boolean isObjectOrArray(byte[] input) {
        return AsciiCharacter.CURLY_BRACKET_OPEN.getAsciiByteValue() == input[0]
                || AsciiCharacter.SQUARE_BRACKET_OPEN.getAsciiByteValue() == input[0];
    }
}
