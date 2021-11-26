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
                    int k = 0; // index based on obfuscation length
                    int unmaskedCharacters = 0; // characters which remained unmasked, difference between obfuscation length and target value length
                    while(inputSliceBytes[i] != UTF8Encoding.DOUBLE_QUOTE.getUtf8ByteValue()) {
                        if (k == obfuscationLength) { // obfuscation length is shorter than input
                            unmaskedCharacters++; // count unmasked characters
                        } else {
                            inputBytes[i + j] = UTF8Encoding.ASTERISK.getUtf8ByteValue();
                            k++;
                        }
                        i++;
                    }
                    if (unmaskedCharacters != 0) {
                        inputBytes = shrinkArray(inputBytes, i, unmaskedCharacters);
                    }
                    if(k < obfuscationLength) { // obfuscation length is longer than input
                        inputBytes = extendArray(inputBytes);
                    }
                    continue outer;
                }
                continue outer;
            }
        }
        return new String(inputBytes, StandardCharsets.UTF_8);
    }

    byte[] shrinkArray(byte[] inputBytes, int index, int unmaskedCharacters) {
        byte[] newInputBytes = new byte[inputBytes.length - unmaskedCharacters];
        System.arraycopy(inputBytes, 0, newInputBytes, 0, index - unmaskedCharacters);
        System.arraycopy(inputBytes, index, newInputBytes, index - unmaskedCharacters, inputBytes.length - index);
        return newInputBytes;
    }

    byte[] extendArray(byte[] inputBytes) {
        byte[] newInputBytes = new byte[inputBytes.length];
        return newInputBytes;
    }
}