package masker.json;

import masker.Utf8AsciiCharacter;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static masker.Utf8AsciiCharacter.isDoubleQuote;
import static masker.Utf8AsciiCharacter.isEscapeCharacter;
import static masker.json.Utf8AsciiJson.*;

public final class KeyContainsMasker implements JsonMaskerImpl {
    private final Set<String> targetKeys;
    private final JsonMaskingConfig maskingConfig;

    public KeyContainsMasker(Set<String> targetKeys, JsonMaskingConfig maskingConfig) {
        this.targetKeys = targetKeys;
        this.maskingConfig = maskingConfig;
    }


    /**
     * Masks the values in the given input for all values having keys corresponding to any of the provided target keys.
     * This implementation is optimized for multiple target keys.
     * Currently, only supports UTF_8/US_ASCII
     * @param input the input message for which values might be masked
     * @return the masked message
     */
    @Override
    public byte[] mask(byte[] input) {
        if (!isObjectOrArray(input) || input.length < 7) {
            // Minimum object required for masking is: {"":""}, so minimum length at least 7 bytes
            // No masking required as first byte is not a '{' or '[', so the JSON input is a value type (boolean, String, number, ...)
            return input;
        }
        int i = 1; // first character can be skipped as it is either a '{' or '['
        // we are looking for targeted JSON keys, so they can appear at minimum 4 characters till the end at the end is: {"":""}
        mainLoop:
        while (i < input.length - 4) {
            // Find JSON strings by looking for unescaped double quotes
            while (!Utf8AsciiCharacter.isDoubleQuote(input[i]) && !Utf8AsciiCharacter.isEscapeCharacter(input[i-1])) {
                if (i < input.length -4) {
                    i++;
                } else {
                    break mainLoop;
                }
            }
            int openingQuoteIndex = i;
            i++; // step over the opening quote
            while (!Utf8AsciiCharacter.isDoubleQuote(input[i]) && !Utf8AsciiCharacter.isEscapeCharacter(input[i-1]) && i < input.length -1) {
                if (i < input.length -4) {
                    i++;
                } else {
                    break mainLoop;
                }
            }
            int closingQuoteIndex = i;
            i++; // step over the closing quote

            // At this point, we found a string ("...").
            // Now let's verify it is a JSON key (it must be followed by a colon (there might be some white spaces in between).
            while (!Utf8AsciiCharacter.isColon(input[i])) {
                if (!Utf8AsciiJson.isWhiteSpace(input[i])) {
                    continue mainLoop; // the found string was not a JSON key, continue looking from where we left of
                }
                i++;
            }

            // At this point, we found a string which is a JSON key.
            // Now let's verify that the value is maskable (a number or string).

            // The index is at the colon between the key and value
            i++; // step over the colon
            while (isWhiteSpace(input[i])) {
                i++; // step over all white characters after the colon
            }
            if (! isDoubleQuote(input[i])) {
                if (maskingConfig.isNumberMaskingDisabled()) {
                    continue mainLoop;  // the found JSON key did not have a maskable value, continue looking from where we left of
                } else {
                    if (! isFirstNumberChar(input[i])) {
                        continue mainLoop; // the found JSON key did not have a maskable value, continue looking from where we left of
                    }
                }
            }

            // At this point, we found a JSON key with a maskable value.
            // Now let's verify the found JSON key is a target key.
            int keyLength = closingQuoteIndex - openingQuoteIndex - 1; // quotes are not included, but it is a length, hence the minus 1
            byte[] keyBytesBuffer = new byte[keyLength];
            System.arraycopy(input, openingQuoteIndex + 1, keyBytesBuffer, 0, keyLength);
            String key = new String(keyBytesBuffer, StandardCharsets.UTF_8);
            if (!targetKeys.contains(key)) {
                continue mainLoop; // The found JSON key is not a target key, so continue the main loop
            }

            // At this point, we found a targeted JSON key with a maskable value.
            // Now let's mask the value.
            int obfuscationLength = maskingConfig.getObfuscationLength();
            if (maskingConfig.isNumberMaskingEnabled() && isNumericCharacter(input[i])) {
                // This block deals with numeric values
                int targetValueLength = 0;
                while (isNumericCharacter(input[i])) {
                    targetValueLength++;
                    input[i] = Utf8AsciiCharacter.toUtf8ByteValue(maskingConfig.getMaskNumberValuesWith());
                    i++;
                    // Following line cannot result in ArrayOutOfBound because of the early return after checking for first char being a double quote

                }
                if (maskingConfig.isObfuscationEnabled() && obfuscationLength != targetValueLength) {
                    if (obfuscationLength == 0) {
                        // For obfuscation length 0, we want to obfuscate numeric values with a single 0 because an empty numeric value is illegal JSON
                        input = FixedLengthValueUtil.setFixedLengthOfValue(input, i, 1, targetValueLength, Utf8AsciiCharacter.toUtf8ByteValue(maskingConfig.getMaskNumberValuesWith()));
                        // The length of the input got changed, so we need to compensate that on our index by stepping the difference between value length and obfuscation length back
                        i = i - (targetValueLength - 1);
                    } else {
                        input = FixedLengthValueUtil.setFixedLengthOfValue(input, i, obfuscationLength, targetValueLength, Utf8AsciiCharacter.toUtf8ByteValue(maskingConfig.getMaskNumberValuesWith()));
                        // The length of the input got changed, so we need to compensate that on our index by stepping the difference between value length and obfuscation length back
                        i = i - (targetValueLength - obfuscationLength);
                    }
                }
            } else {
                // This block deals with Strings
                i++; // step over quote
                int targetValueLength = 0;
                int noOfEscapeCharacters = 0;
                boolean escapeNextCharacter = false;
                boolean previousCharacterCountedAsEscapeCharacter = false;
                while (! isDoubleQuote(input[i]) || (isDoubleQuote(input[i]) && escapeNextCharacter)) {
                    escapeNextCharacter = isEscapeCharacter(input[i]);
                    if (escapeNextCharacter && !previousCharacterCountedAsEscapeCharacter) {
                        noOfEscapeCharacters++;
                        previousCharacterCountedAsEscapeCharacter = true;
                    } else {
                        previousCharacterCountedAsEscapeCharacter = false;
                    }
                    input[i] = Utf8AsciiCharacter.ASTERISK.getUtf8ByteValue();
                    targetValueLength++;
                    i++;
                }
                if (maskingConfig.isObfuscationEnabled() && obfuscationLength != (targetValueLength - noOfEscapeCharacters)) {
                    input = FixedLengthValueUtil.setFixedLengthOfStringValue(input, i, obfuscationLength, targetValueLength); // set reference of input bytes to the new array reference
                    // compensate the index for shortening the input by setting fixed length to be at the closing double quote of the String value
                    i = i - (targetValueLength - obfuscationLength);
                }  else if (noOfEscapeCharacters > 0) { // So we don't add asterisks for escape characters (which are not part of the String length)
                    int actualStringLength = targetValueLength - noOfEscapeCharacters;
                    input = FixedLengthValueUtil.setFixedLengthOfStringValue(input, i, actualStringLength, targetValueLength);
                    // compensate the index for shortening the input with noOfEscapeCharacters to be at closing double quote of the String value
                    i = i - noOfEscapeCharacters;
                }
                i++; // step over closing quote of string value to start looking for the next JSON key
            }
        }
        return input;
    }

    private boolean isObjectOrArray(byte[] input) {
        return Utf8AsciiCharacter.CURLY_BRACKET_OPEN.getUtf8ByteValue() == input[0] || Utf8AsciiCharacter.SQUARE_BRACKET_OPEN.getUtf8ByteValue() == input[0];
    }
}
