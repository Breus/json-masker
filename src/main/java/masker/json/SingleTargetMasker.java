package masker.json;

import masker.UTF8Encoding;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SingleTargetMasker implements JsonMaskerImpl {
    private final Set<String> quotedTargetKeys;
    private final JsonMaskingConfig maskingConfig;

    public SingleTargetMasker(Set<String> targetKeys, JsonMaskingConfig maskingConfig) {
        this.quotedTargetKeys = new HashSet<>();
        targetKeys.forEach(t -> quotedTargetKeys.add('"' + t + '"'));
        this.maskingConfig = maskingConfig;
    }

    @NotNull
    @Override
    public String mask(@NotNull String input) {
        return new String(mask(input.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    @Override
    public byte[] mask(byte[] message) {
        for (String targetKey : quotedTargetKeys) {
            message = mask(message, targetKey);
        }
        return message;
    }

    /**
     * Masks the String values in the given input for all values corresponding to the provided target key.
     * This implementation is optimized for a single target key.
     * Currently, only supports UTF_8/US_ASCII
     *
     * @param input     the UTF-8 encoded input bytes for which values of the target key are masked
     * @param targetKey the JSON key for which the String values are masked
     * @return the masked message
     */
    public byte[] mask(byte[] input, @NotNull String targetKey) {
        byte[] outputBytes = input; // with obfuscation enabled, this is used as output (so can eventually have different length than originalInput)
        int i = 0; // index based on current input slice
        int j = 0; // index based on input
        outer: while (j < outputBytes.length - targetKey.getBytes().length - 2) { // minus 1 for closing bracket, smaller than because colon required for a new key and minus 1 for value with minimum length of 1
            j = j + i;
            byte[] inputSliceBytes;
            if (j == 0) {
                inputSliceBytes = outputBytes;
            } else {
                inputSliceBytes = Arrays.copyOfRange(input, j, input.length);
            }
            int startIndexOfTargetKey = indexOf(inputSliceBytes, targetKey.getBytes(StandardCharsets.UTF_8));
            if(startIndexOfTargetKey == -1) {
                break; // input doesn't contain target key anymore, no further masking required
            }
            i = startIndexOfTargetKey + targetKey.length(); // step over found target key
            for (; i < inputSliceBytes.length; i++) {
                if (UTF8JsonCharacters.isWhiteSpace(inputSliceBytes[i])) {
                    continue; // found a JSON whitespace, try next character
                }
                if (inputSliceBytes[i] == UTF8Encoding.COLON.getUtf8ByteValue()) {
                    break; // found a colon, so the found target key is indeed a JSON key
                }
                continue outer; // found a different character than whitespace or colon, so the found target key is not a JSON key
            }
            i++; // step over colon
            for (; i < inputSliceBytes.length; i++) {
                if (UTF8JsonCharacters.isWhiteSpace(inputSliceBytes[i])) {
                    continue; // found a space, try next character
                }
                if (inputSliceBytes[i] == UTF8Encoding.DOUBLE_QUOTE.getUtf8ByteValue()) { // value is a string
                    i++; // step over quote
                    int targetValueLength = 0;
                    boolean escapeNextCharacter = false;
                    while(inputSliceBytes[i] != UTF8Encoding.DOUBLE_QUOTE.getUtf8ByteValue() || (inputSliceBytes[i] == UTF8Encoding.DOUBLE_QUOTE.getUtf8ByteValue() && escapeNextCharacter)) {
                        escapeNextCharacter = (inputSliceBytes[i] == UTF8Encoding.BACK_SLASH.getUtf8ByteValue());
                        outputBytes[i + j] = UTF8Encoding.ASTERISK.getUtf8ByteValue();
                        targetValueLength++;
                        i++;
                    }
                    int obfuscationLength = maskingConfig.getObfuscationLength();
                    if (obfuscationLength != -1 && obfuscationLength != targetValueLength) {
                        outputBytes = LengthObfuscationUtil.obfuscateLengthOfTargetValue(outputBytes, i, obfuscationLength, targetValueLength); // set reference of input bytes to the new array reference
                    }
                }
                continue outer;
            }
        }
        return outputBytes;
    }




    private int indexOf(byte[] src, byte[] target) {
        for (int i = 0; i <= src.length - target.length; i++) {
            boolean found = true;
            for (int j = 0; j < target.length; ++j) {
                if (src[i + j] != target[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1;
    }
}
