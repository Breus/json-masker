package masker.json;

import masker.Utf8AsciiCharacter;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Set;

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
        int i = 0;
        outer:
        while (i < input.length - 2) { // minus one character for closing curly bracket, one for the value
            if (input[i] != Utf8AsciiCharacter.COLON.getUtf8ByteValue()) {  // loop until index is on colon (to find a JSON key)
                i++;
                continue;
            }
            int colonIndex = i;
            i--; // step back from colon
            while (input[i] != Utf8AsciiCharacter.DOUBLE_QUOTE.getUtf8ByteValue()) {
                i--; // loop back until index is on closing quote of JSON key
            }
            int closingQuoteIndex = i;
            i--; // step back from closing quote
            while (input[i] != Utf8AsciiCharacter.DOUBLE_QUOTE.getUtf8ByteValue() && input[i-1] != Utf8AsciiCharacter.BACK_SLASH.getUtf8ByteValue()) {
                i--; // loop back until index is on opening quote of key, so ignore escaped quotes (which are part of the value)
            }
            int openingQuoteIndex = i;
            int keyLength = closingQuoteIndex - openingQuoteIndex - 1; // quotes are not included, but it is a length, hence the minus 1
            byte[] keyBytes = new byte[keyLength];
            System.arraycopy(input, openingQuoteIndex + 1, keyBytes, 0, keyLength);
            String key = new String(keyBytes, StandardCharsets.UTF_8);
            i = colonIndex + 1; // continue looping from after colon
            if (!targetKeys.contains(key)) {
                i = i + 4; // +4 since minimum amount of characters between colon is 5: {"a":1,"":2}
                continue;
            }

            while (unrecognizedValueCharacter(input[i])) {
                if (Utf8AsciiJson.isWhiteSpace(input[i])) {
                    i++; // skip white characters
                    continue;
                }
                i++;
                continue outer; // any other character than white space or double quote means the value is not a string, so we don't have to do any masking. Only if masking number values is set, we also move on when next character is a numeric character
            }
            int obfuscationLength = maskingConfig.getObfuscationLength();
            if (maskingConfig.getMaskNumberValuesWith() == -1 || ! Utf8AsciiJson.isNumericCharacter(input[i])) {
                // This block deals with Strings
                i++; // step over quote
                int targetValueLength = 0;
                boolean escapeNextCharacter = false;
                while (input[i] != Utf8AsciiCharacter.DOUBLE_QUOTE.getUtf8ByteValue() || (input[i] == Utf8AsciiCharacter.DOUBLE_QUOTE.getUtf8ByteValue() && escapeNextCharacter)) {
                    escapeNextCharacter = (input[i] == Utf8AsciiCharacter.BACK_SLASH.getUtf8ByteValue());
                    input[i] = Utf8AsciiCharacter.ASTERISK.getUtf8ByteValue();
                    targetValueLength++;
                    i++;
                }
                if (obfuscationLength != -1 && obfuscationLength != targetValueLength) {
                    input = LengthObfuscationUtil.obfuscateLengthOfStringValue(input, i, obfuscationLength, targetValueLength); // set reference of input bytes to the new array reference
                }
            } else {
                // This block deals with numeric values
                int targetValueLength = 0;
                while (Utf8AsciiJson.isNumericCharacter(input[i])) {
                    targetValueLength++;
                    input[i] = Utf8AsciiCharacter.toUtf8ByteValue(maskingConfig.getMaskNumberValuesWith());
                    i++;
                }
                if (obfuscationLength != -1 && obfuscationLength != targetValueLength) {
                    if (obfuscationLength == 0) {
                        // For obfuscation length 0, we want to obfuscate numeric values with a single 0 because an empty numeric value is illegal JSON
                        input = LengthObfuscationUtil.obfuscationLengthOfValue(input, i, 1, targetValueLength, Utf8AsciiCharacter.toUtf8ByteValue(maskingConfig.getMaskNumberValuesWith()));
                    } else {
                        input = LengthObfuscationUtil.obfuscationLengthOfValue(input, i, obfuscationLength, targetValueLength, Utf8AsciiCharacter.toUtf8ByteValue(maskingConfig.getMaskNumberValuesWith()));
                    }
                }
            }
        }
        return input;
    }

    /**
     * Wrong character is anything that is not a quotation mark (denoting String value beginnings) or not numeric in case number masking is enabled.
     * @param inputByte the character to check
     * @return true if it's an unrecognized value character or else false
     */
    private boolean unrecognizedValueCharacter(@NotNull byte inputByte) {
        if (maskingConfig.getMaskNumberValuesWith() != -1 && Utf8AsciiJson.isNumericCharacter(inputByte)) {
            return false;
        }
        return ! (Utf8AsciiCharacter.DOUBLE_QUOTE.getUtf8ByteValue() == inputByte);
    }

    @NotNull
    @Override
    public String mask(@NotNull String input) {
        return new String(mask(input.getBytes(StandardCharsets.UTF_8)),StandardCharsets.UTF_8);
    }
}
