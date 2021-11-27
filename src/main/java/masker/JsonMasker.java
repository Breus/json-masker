package masker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

final class JsonMasker extends AbstractMasker {
    private final Set<String> quotedTargetKeys;

    @NotNull
    public static JsonMasker getDefaultMasker(@NotNull String targetKey) {
        return getDefaultMasker(Set.of(targetKey));
    }

    @NotNull
    public static JsonMasker getDefaultMasker(@NotNull Set<String> targetKeys) {
        return getMasker(targetKeys, MaskingConfig.defaultConfig());
    }

    @NotNull
    public static JsonMasker getMasker(@NotNull String targetKey, @Nullable MaskingConfig maskingConfig) {
        return getMasker(Set.of(targetKey), maskingConfig);
    }

    @NotNull
    public static JsonMasker getMasker(@NotNull Set<String> targetKeys, @Nullable MaskingConfig maskingConfig) {
        if (maskingConfig == null) {
            return new JsonMasker(targetKeys, MaskingConfig.defaultConfig());
        }
        return new JsonMasker(targetKeys, maskingConfig);
    }
    
    @Override
    public byte[] mask(byte[] message, @NotNull Charset charset) {
        for (String targetKey : getQuotedTargetKeys()) {
            message = maskValuesOfTargetKey(new String(message, charset), targetKey).getBytes(charset);
        }
        return message;
    }

    @Override
    @NotNull
    public String mask(@NotNull String message) {
        for (String targetKey : getQuotedTargetKeys()) {
            message = maskValuesOfTargetKey(message, targetKey);
        }
        return message;
    }

    private JsonMasker(@NotNull Set<String> targetKeys, @NotNull MaskingConfig maskingConfiguration) {
        super(targetKeys, maskingConfiguration);
        Set<String> quotedTargetKeys = new HashSet<>();
        targetKeys.forEach(t -> quotedTargetKeys.add('"' + t + '"'));
        this.quotedTargetKeys = quotedTargetKeys;
    }

    @NotNull
    String  maskValuesOfTargetKey(@NotNull String input, @NotNull String targetKey) {
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        int i = 0; // index based on current input slice
        int j = 0; // index based on input
        outer: while (j < inputBytes.length - targetKey.length() - 1) { // minus 1 for closing bracket, smaller than because colon required for a new key which has a value (number).
            j = j + i;
            String inputSlice;
            byte[] inputSliceBytes;
            if (j == 0) {
                inputSlice = input;
                inputSliceBytes = inputBytes;
            } else {
                inputSlice = input.substring(j);
                inputSliceBytes = inputSlice.getBytes(StandardCharsets.UTF_8);
            }
            int startIndexOfTargetKey = inputSlice.indexOf(targetKey);
            if(startIndexOfTargetKey == -1) {
                break; // input doesn't contain target key anymore, no further masking required
            }
            i = startIndexOfTargetKey + targetKey.length();
            for (; i < inputSliceBytes.length; i++) {
                if (inputSliceBytes[i] == UTF8Encoding.SPACE.getUtf8ByteValue()) {
                    continue; // found a space, try next character
                }
                if (inputSliceBytes[i] == UTF8Encoding.COLON.getUtf8ByteValue()) {
                    break; // found a colon, so the found target key is indeed a JSON key
                }
                continue outer; // found a different character than whitespace or colon, so the found target key is not a JSON key
            }
            i++; // step over colon
            for (; i < inputSliceBytes.length; i++) {
                if (inputSliceBytes[i] == UTF8Encoding.SPACE.getUtf8ByteValue()) {
                    continue; // found a space, try next character
                }
                if (inputSliceBytes[i] == UTF8Encoding.DOUBLE_QUOTE.getUtf8ByteValue()) { // value is a string
                    i++; // step over quote
                    int targetValueLength = 0;
                    while(inputSliceBytes[i] != UTF8Encoding.DOUBLE_QUOTE.getUtf8ByteValue()) {
                        inputBytes[i + j] = UTF8Encoding.ASTERISK.getUtf8ByteValue();
                        targetValueLength++;
                        i++;
                    }
                    int obfuscationLength = getMaskingConfiguration().getObfuscationLength();
                    if (obfuscationLength != -1 && obfuscationLength != targetValueLength) {
                        inputBytes = obfuscateLengthOfTargetValue(inputBytes, i, obfuscationLength, targetValueLength); // set reference of input bytes to the new array reference
                    }
                    continue outer;
                }
                continue outer;
            }
        }
        return new String(inputBytes, StandardCharsets.UTF_8);
    }

    byte[] obfuscateLengthOfTargetValue(byte[] inputBytes, int closingQuoteIndex, int obfuscationLength, int targetValueLength) {
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
}