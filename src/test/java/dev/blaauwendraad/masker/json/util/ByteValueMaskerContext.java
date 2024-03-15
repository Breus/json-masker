package dev.blaauwendraad.masker.json.util;

import dev.blaauwendraad.masker.json.ValueMaskerContext;

import java.nio.charset.StandardCharsets;

public class ByteValueMaskerContext implements ValueMaskerContext {
    private byte[] value;

    public ByteValueMaskerContext(byte[] value) {
        this.value = value;
    }

    public ByteValueMaskerContext(String value) {
        this(value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public byte getByte(int index) {
        return value[index];
    }

    @Override
    public int valueLength() {
        return value.length;
    }

    @Override
    public void replaceValue(int fromIndex, int length, byte[] mask, int maskRepeat) {
        int suffixLength = value.length - (length + fromIndex);
        int newArraySize = fromIndex + (mask.length * maskRepeat) + suffixLength;
        byte[] maskedValue = new byte[newArraySize];
        // copy the prefix
        System.arraycopy(
                value,
                0,
                maskedValue,
                0,
                fromIndex
        );
        // copy the mask(s)
        for (int i = 0; i < maskRepeat; i++) {
            System.arraycopy(
                    mask,
                    0,
                    maskedValue,
                    fromIndex + i * mask.length,
                    mask.length
            );
        }
        // copy the suffix
        System.arraycopy(
                value,
                fromIndex + length,
                maskedValue,
                maskedValue.length - suffixLength,
                suffixLength
        );

        this.value = maskedValue;
    }

    @Override
    public int countNonVisibleCharacters(int fromIndex, int length) {
        return Utf8Util.countNonVisibleCharacters(value, fromIndex, length);
    }

    public String asString() {
        return new String(value, StandardCharsets.UTF_8);
    }
}
