package dev.blaauwendraad.masker.json.util;

import dev.blaauwendraad.masker.json.ValueMaskerContext;

import java.nio.charset.StandardCharsets;

/**
 * {@link ValueMaskerContext} implementation that uses a byte array as the value. Used only for testing purposes due
 * to absence of {@code MaskingState} when masking with Jackson.
 */
public class ByteValueMaskerContext implements ValueMaskerContext {
    private final byte[] value;
    private byte[] maskedValue;

    public ByteValueMaskerContext(byte[] value) {
        this.value = value;
        this.maskedValue = value;
    }

    public ByteValueMaskerContext(String value) {
        this(value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public byte getByte(int index) {
        return value[index];
    }

    @Override
    public int byteLength() {
        return value.length;
    }

    @Override
    public void replaceBytes(int fromIndex, int length, byte[] mask, int maskRepeat) {
        int suffixLength = maskedValue.length - (length + fromIndex);
        int newArraySize = fromIndex + (mask.length * maskRepeat) + suffixLength;
        byte[] newMaskedValue = new byte[newArraySize];
        // copy the prefix
        System.arraycopy(
                maskedValue,
                0,
                newMaskedValue,
                0,
                fromIndex
        );
        // copy the mask(s)
        for (int i = 0; i < maskRepeat; i++) {
            System.arraycopy(
                    mask,
                    0,
                    newMaskedValue,
                    fromIndex + i * mask.length,
                    mask.length
            );
        }
        // copy the suffix
        System.arraycopy(
                maskedValue,
                fromIndex + length,
                newMaskedValue,
                newMaskedValue.length - suffixLength,
                suffixLength
        );

        this.maskedValue = newMaskedValue;
    }

    @Override
    public int countNonVisibleCharacters(int fromIndex, int length) {
        return Utf8Util.countNonVisibleCharacters(value, fromIndex, length);
    }

    @Override
    public String asText() {
        if (value[0] == '"') {
            // remove quotes from the string value
            return new String(value, 1, value.length - 2, StandardCharsets.UTF_8);
        }
        return new String(value, StandardCharsets.UTF_8);
    }

    public String getMaskedValue() {
        return new String(maskedValue, StandardCharsets.UTF_8);
    }
}
