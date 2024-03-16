package dev.blaauwendraad.masker.json.util;

import dev.blaauwendraad.masker.json.ValueMaskerContext;

import java.nio.charset.StandardCharsets;

/**
 * {@link ValueMaskerContext} implementation that uses a byte array as the value. Used only for testing purposes due
 * to absence of global {@link dev.blaauwendraad.masker.json.MaskingState} when masking with Jackson.
 */
public class ByteValueMaskerContext implements ValueMaskerContext {
    private final byte[] value;
    private byte[] maskedValue;

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
        if (this.maskedValue != null) {
            throw new IllegalStateException("Value already masked, this implementation does not support that");
        }
        int suffixLength = value.length - (length + fromIndex);
        int newArraySize = fromIndex + (mask.length * maskRepeat) + suffixLength;
        this.maskedValue = new byte[newArraySize];
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
        if (maskedValue == null) {
            return new String(value, StandardCharsets.UTF_8);
        }
        return new String(maskedValue, StandardCharsets.UTF_8);
    }
}
