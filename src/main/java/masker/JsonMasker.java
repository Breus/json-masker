package masker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

final class JsonMasker extends AbstractMasker {
    @NotNull
    public static JsonMasker getDefaultMasker(@NotNull String targetKey) {
        return getMasker(targetKey, null);
    }

    @NotNull
    public static JsonMasker getMasker(@NotNull String targetKey, @Nullable MaskingConfig maskingConfiguration) {
        return new JsonMasker(targetKey, maskingConfiguration);
    }
    
    @Override
    public byte[] mask(byte[] message, @NotNull Charset charset) {
        return maskValuesOfTargetKey(new String(message, charset)).getBytes(charset);
    }

    @Override
    @NotNull
    public String mask(@NotNull String message) {
        return maskValuesOfTargetKey(message);
    }

    private JsonMasker(@NotNull String targetKey, @Nullable MaskingConfig maskingConfiguration) {
        super("\"" + targetKey + "\"", maskingConfiguration);
    }

    @NotNull
    String  maskValuesOfTargetKey(@NotNull String input) {
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        int i = 0; // index based on current input slice
        int j = 0; // index based on input
        outer: while (j < inputBytes.length - getTargetKeyLength() - 1) { // minus 1 for closing '}', and < for ':' required for a new key which has a value (number).
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
            int startIndexOfTargetKey = inputSlice.indexOf(super.getTargetKey());
            if(startIndexOfTargetKey == -1) {
                break; // input doesn't contain target key anymore, no further masking required
            }
            i = startIndexOfTargetKey + super.getTargetKeyLength();
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
                    int obfuscationLength = getMaskingConfiguration().getObfuscationLength();
                    int targetValueLength = 0;
                    while(inputSliceBytes[i] != UTF8Encoding.DOUBLE_QUOTE.getUtf8ByteValue()) {
                        if (obfuscationLength != -1) {
                            targetValueLength++;
                        } else {
                            inputBytes[i + j] = UTF8Encoding.ASTERISK.getUtf8ByteValue();
                        }
                        i++;
                    }
                    if (obfuscationLength != -1) {
                        inputBytes = modifyArray(inputBytes, i, obfuscationLength, targetValueLength);
                    }
                    continue outer;
                }
                continue outer;
            }
        }
        return new String(inputBytes, StandardCharsets.UTF_8);
    }

    byte[] modifyArray(byte[] inputBytes, int index, int obfuscationLength, int targetValueLength) {
        byte[] newInputBytes = new byte[inputBytes.length + (obfuscationLength - targetValueLength)];
        System.arraycopy(inputBytes, 0, newInputBytes, 0, index - targetValueLength);
        for (int i = index - targetValueLength; i < index - targetValueLength + obfuscationLength; i++) {
            newInputBytes[i] = UTF8Encoding.ASTERISK.getUtf8ByteValue();
        }
        System.arraycopy(inputBytes, index, newInputBytes, index - targetValueLength + obfuscationLength, inputBytes.length - index);
        return newInputBytes;
    }
}