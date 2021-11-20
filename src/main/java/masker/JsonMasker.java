package masker;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

class JsonMasker implements MessageMasker {
    @NotNull
    public static JsonMasker getMaskerWithTargetKey(@NotNull String targetKey) {
        return new JsonMasker(targetKey);
    }

    @Override
    public byte[] mask(byte[] message, @NotNull Charset charset) {
        return maskValuesOfTargetKey("", new String(message, charset)).getBytes(charset);
    }

    @Override
    @NotNull
    public String mask(@NotNull String message) {
        return maskValuesOfTargetKey("", message);
    }

    String targetKey;
    int targetKeyLength;

    private JsonMasker(@NotNull String targetKey) {
        if (targetKey.length() < 1) {
            throw new IllegalArgumentException("Target key must at least contain a single character");
        }
        this.targetKey = "\"" + targetKey + "\"";
        this.targetKeyLength = getTargetKey().length();
    }

    @NotNull
    String maskValuesOfTargetKey(@NotNull String prefix, @NotNull String input) {
        int startIndexOfFilterKey = input.indexOf(getTargetKey());
        if (startIndexOfFilterKey == -1) {
            return input; // input doesn't contain filter key, so no need to mask anything
        }
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        int colonIndex = 0;
        int i = startIndexOfFilterKey + getTargetKeyLength();
        for (; i < inputBytes.length; i++) {
            if (inputBytes[i] == getByteValueOfUTF8String(":")) {
                colonIndex = i;
                break;
            } else if (inputBytes[i] == getByteValueOfUTF8String(" ")) {
                continue;
            } else {
                break;
            }
        }
        if (colonIndex == 0) {
            return maskValuesOfTargetKey(input.substring(0,i), input.substring(i)); // input contained filter key, but it wasn't a JSON key, so continue on the tail
        }
        i++; // step over colon
        for (; i < inputBytes.length; i++) {
            if (inputBytes[i] == getByteValueOfUTF8String("\"")) {
                i++; // skip over quote
                while(inputBytes[i] != getByteValueOfUTF8String("\"")) {
                    inputBytes[i] = getByteValueOfUTF8String("*");
                    i++;
                }
                break;
            } else if (inputBytes[i] == getByteValueOfUTF8String(" ")) {
                continue;
            } else {
                return maskValuesOfTargetKey(input.substring(0, i), input.substring(i));
            }
        }
        return prefix + new String(inputBytes, StandardCharsets.UTF_8);
    }

     byte getByteValueOfUTF8String(@NotNull String inputStringCharacter) {
        if (inputStringCharacter.length() != 1) {
            throw new IllegalArgumentException("This method should only be called for Strings which are only a single byte in UTF-8");
        }
        return inputStringCharacter.getBytes(StandardCharsets.UTF_8)[0];
    }

    String getTargetKey() {
        return targetKey;
    }

    int getTargetKeyLength() {
        return targetKeyLength;
    }
}