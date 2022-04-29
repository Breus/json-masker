package masker.json;

import masker.UTF8Encoding;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class KeyContainsMaskingAlgorithm {

    private KeyContainsMaskingAlgorithm() {
        // don't instantiate
    }

    /**
     * Masks the String values in the given input for all values having keys corresponding to any of the provided target keys.
     * This implementation is optimized for multiple target keys.
     * Currently, only supports UTF_8/US_ASCII
     * @param input the input message for which values might be masked
     * @param targetKeys the set of JSON keys for which the String values are masked
     * @param maskingConfig the JSON masking configuration
     * @return the masked message
     */
    @NotNull
    static String mask(@NotNull String input, @NotNull Set<String> targetKeys, @NotNull JsonMaskingConfig maskingConfig) {
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        int i = 0;
        outer:
        while (i < inputBytes.length - 2) { // minus one character for closing curly bracket, one for the value
            if (inputBytes[i] != UTF8Encoding.COLON.getUtf8ByteValue()) {  // loop until index is on colon
                i++;
                continue;
            }
            int colonIndex = i;
            i--; // step back from colon
            while (inputBytes[i] != UTF8Encoding.DOUBLE_QUOTE.getUtf8ByteValue()) {
                i--; // loop back until index is on closing quote of JSON key
            }
            int closingQuoteIndex = i;
            i--; // step back from closing quote
            while (inputBytes[i] != UTF8Encoding.DOUBLE_QUOTE.getUtf8ByteValue() && inputBytes[i-1] != UTF8Encoding.BACK_SLASH.getUtf8ByteValue()) {
                i--; // loop back until index is on opening quote of key, so ignore escaped quotes (which are part of the value)
            }
            int openingQuoteIndex = i;
            int keyLength = closingQuoteIndex - openingQuoteIndex - 1; // quotes are not included, but it is a length, hence the minus 1
            byte[] keyBytes = new byte[keyLength];
            System.arraycopy(inputBytes, openingQuoteIndex + 1, keyBytes, 0, keyLength);
            String key = new String(keyBytes, StandardCharsets.UTF_8);
            i = colonIndex + 1; // continue looping from after colon
            if (!targetKeys.contains(key)) {
                i = i + 4; // +4 since minimum amount of characters between colon is 5: {"a":1,"":2}
                continue;
            }

            while (inputBytes[i] != UTF8Encoding.DOUBLE_QUOTE.getUtf8ByteValue()) {
                if (UTF8JsonCharacters.isWhiteSpace(inputBytes[i])) {
                    i++; // skip white characters
                    continue;
                }
                i++;
                continue outer; // any other character than white space or double quote means the value is not a string, so we don't have to do any masking
            }
            i++; // step over quote
            int targetValueLength = 0;
            boolean escapeNextCharacter = false;
            while(inputBytes[i] != UTF8Encoding.DOUBLE_QUOTE.getUtf8ByteValue() || (inputBytes[i] == UTF8Encoding.DOUBLE_QUOTE.getUtf8ByteValue() && escapeNextCharacter)) {
                escapeNextCharacter = (inputBytes[i] == UTF8Encoding.BACK_SLASH.getUtf8ByteValue());
                inputBytes[i] = UTF8Encoding.ASTERISK.getUtf8ByteValue();
                targetValueLength++;
                i++;
            }
            int obfuscationLength = maskingConfig.getObfuscationLength();
            if (obfuscationLength != -1 && obfuscationLength != targetValueLength) {
                inputBytes = JsonMasker.obfuscateLengthOfTargetValue(inputBytes, i, obfuscationLength, targetValueLength); // set reference of input bytes to the new array reference
            }
        }
        return new String(inputBytes, StandardCharsets.UTF_8);
    }
}
