package masker.json;

import masker.AbstractMasker;
import masker.UTF8Encoding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class JsonMasker extends AbstractMasker {
    private final JsonMaskingConfig maskingConfig;
    private Set<String> quotedTargetKeys; // TODO: only used for SINGLE_TARGET_LOOP algorithm, might want to remove somehow

    @NotNull
    public static JsonMasker getMasker(@NotNull String targetKey) {
        return getMasker(targetKey, null);
    }

    @NotNull
    public static JsonMasker getMasker(@NotNull String targetKey, @Nullable JsonMaskingConfig maskingConfig) {
        return getMasker(Set.of(targetKey), maskingConfig);
    }

    @NotNull
    public static JsonMasker getMasker(@NotNull Set<String> targetKeys) {
        return getMasker(targetKeys, null);
    }

    @NotNull
    public static JsonMasker getMasker(@NotNull Set<String> targetKeys, @Nullable JsonMaskingConfig maskingConfig) {
        return new JsonMasker(targetKeys, maskingConfig);
    }
    
    @Override
    public byte[] mask(byte[] message, @NotNull Charset charset) {
        for (String targetKey : getQuotedTargetKeys()) {
            message = mask(new String(message, charset), targetKey).getBytes(charset);
        }
        return message;
    }

    @Override
    @NotNull
    public String mask(@NotNull String message) {
        if (getMaskingConfig().getMultiTargetAlgorithm() == JsonMultiTargetAlgorithm.SINGLE_TARGET_LOOP) {
            for (String targetKey : getQuotedTargetKeys()) {
                message = mask(message, targetKey);
            }
        } else if (getMaskingConfig().getMultiTargetAlgorithm() == JsonMultiTargetAlgorithm.KEYS_CONTAIN) {
            message = KeyContainsMaskingAlgorithm.mask(message, getTargetKeys(), getMaskingConfig());
        }
        return message;
    }

    private JsonMasker(@NotNull Set<String> targetKeys, @Nullable JsonMaskingConfig maskingConfig) {
        super(targetKeys);
        if (maskingConfig == null) {
            this.maskingConfig = JsonMaskingConfig.getDefault();
        } else {
            this.maskingConfig = maskingConfig;
        }
        if (this.maskingConfig.getMultiTargetAlgorithm() == JsonMultiTargetAlgorithm.SINGLE_TARGET_LOOP) {
            Set<String> quotedTargetKeys = new HashSet<>();
            targetKeys.forEach(t -> quotedTargetKeys.add('"' + t + '"'));
            this.quotedTargetKeys = quotedTargetKeys;
        }
    }

    /**
     * @see JsonMasker#mask(byte[], String)
     */
    String mask(@NotNull String input, @NotNull String targetKey) {
        return new String(mask(input.getBytes(StandardCharsets.UTF_8), targetKey), StandardCharsets.UTF_8);
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
    byte[] mask(byte[] input, @NotNull String targetKey) {
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
                    int obfuscationLength = getMaskingConfig().getObfuscationLength();
                    if (obfuscationLength != -1 && obfuscationLength != targetValueLength) {
                        outputBytes = obfuscateLengthOfTargetValue(outputBytes, i, obfuscationLength, targetValueLength); // set reference of input bytes to the new array reference
                    }
                    continue outer;
                }
                continue outer;
            }
        }
        return outputBytes;
    }

    static byte[] obfuscateLengthOfTargetValue(byte[] inputBytes, int closingQuoteIndex, int obfuscationLength, int targetValueLength) {
        byte[] newInputBytes = new byte[inputBytes.length + (obfuscationLength - targetValueLength)]; // create new empty array with a length computed by the difference between obfuscation and target value length
        int targetValueStartIndex = closingQuoteIndex - targetValueLength;
        System.arraycopy(inputBytes, 0, newInputBytes, 0, targetValueStartIndex); // copy all bytes till the target value (including opening quotes)
        for (int i = targetValueStartIndex; i < targetValueStartIndex + obfuscationLength; i++) { // start from beginning of target value and loop amount of wanted masked characters
            newInputBytes[i] = UTF8Encoding.ASTERISK.getUtf8ByteValue(); // add masking characters
        }
        System.arraycopy(inputBytes, closingQuoteIndex, newInputBytes, targetValueStartIndex + obfuscationLength, inputBytes.length - closingQuoteIndex); // append rest of the original array starting from end of target value
        return newInputBytes;
    }

    public Set<String> getQuotedTargetKeys() {
        return quotedTargetKeys;
    }

    public JsonMaskingConfig getMaskingConfig() {
        return maskingConfig;
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