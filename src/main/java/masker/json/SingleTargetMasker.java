package masker.json;

import masker.Utf8AsciiCharacter;
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
                if (Utf8AsciiJson.isWhiteSpace(inputSliceBytes[i])) {
                    continue; // found a JSON whitespace, try next character
                }
                if (inputSliceBytes[i] == Utf8AsciiCharacter.COLON.getUtf8ByteValue()) {
                    break; // found a colon, so the found target key is indeed a JSON key
                }
                continue outer; // found a different character than whitespace or colon, so the found target key is not a JSON key
            }
            i++; // step over colon
            for (; i < inputSliceBytes.length; i++) {
                if (Utf8AsciiJson.isWhiteSpace(inputSliceBytes[i])) {
                    continue; // found a space, try next character
                }
                // If number masking is enabled, check if the next character could be the first character of a JSON number value
                if (maskingConfig.isNumberMaskingEnabled() && Utf8AsciiJson.isFirstNumberChar(inputSliceBytes[i]))  {
                    int targetValueLength = 0;
                    while (Utf8AsciiJson.isNumericCharacter(inputSliceBytes[i])) {
                        outputBytes[i + j] = Utf8AsciiCharacter.toUtf8ByteValue(maskingConfig.getMaskNumberValuesWith());
                        targetValueLength++;
                        i++;
                    }
                    int obfuscationLength = maskingConfig.getObfuscationLength();
                    if (maskingConfig.isObfuscationEnabled() && obfuscationLength != targetValueLength) {
                        if (obfuscationLength == 0) {
                            // For obfuscation length 0, we want to obfuscate numeric values with a single 0 because an empty numeric value is illegal JSON
                            outputBytes = FixedLengthValueUtil.setFixedLengthOfValue(outputBytes, i, 1, targetValueLength, Utf8AsciiCharacter.toUtf8ByteValue(maskingConfig.getMaskNumberValuesWith()));
                        } else {
                            outputBytes = FixedLengthValueUtil.setFixedLengthOfValue(outputBytes, i, obfuscationLength, targetValueLength, Utf8AsciiCharacter.toUtf8ByteValue(maskingConfig.getMaskNumberValuesWith()));
                        }
                    }
                }

                if (inputSliceBytes[i] == Utf8AsciiCharacter.DOUBLE_QUOTE.getUtf8ByteValue()) { // value is a string
                    i++; // step over quote
                    int targetValueLength = 0;
                    int noOfEscapeCharacters = 0;
                    boolean escapeNextCharacter = false;
                    while(inputSliceBytes[i] != Utf8AsciiCharacter.DOUBLE_QUOTE.getUtf8ByteValue() || (inputSliceBytes[i] == Utf8AsciiCharacter.DOUBLE_QUOTE.getUtf8ByteValue() && escapeNextCharacter)) {
                        escapeNextCharacter = (inputSliceBytes[i] == Utf8AsciiCharacter.BACK_SLASH.getUtf8ByteValue() && !escapeNextCharacter);
                        if (escapeNextCharacter) {
                            noOfEscapeCharacters++;
                        }
                        outputBytes[i + j] = Utf8AsciiCharacter.ASTERISK.getUtf8ByteValue();
                        targetValueLength++;
                        i++;
                    }
                    int obfuscationLength = maskingConfig.getObfuscationLength();
                    if (maskingConfig.isObfuscationEnabled() && obfuscationLength != targetValueLength - obfuscationLength) {
                        outputBytes = FixedLengthValueUtil.setFixedLengthOfStringValue(outputBytes, i, obfuscationLength, targetValueLength); // set reference of input bytes to the new array reference
                    } else if (noOfEscapeCharacters > 0) { // So we don't add asterisks for escape characters (which are not part of the String length)
                        int actualStringLength = targetValueLength - noOfEscapeCharacters;
                        outputBytes = FixedLengthValueUtil.setFixedLengthOfStringValue(input, i, actualStringLength, targetValueLength);
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
