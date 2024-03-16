package dev.blaauwendraad.masker.json;

/**
 * Represents a value that is currently being masked. The context is passed to {@link ValueMasker#maskValue(ValueMaskerContext)}
 * to allow the implementation to replace the JSON value with a mask.
 */
public interface ValueMaskerContext {
    /**
     * Retrieve the byte at the given index.
     */
    byte getByte(int index);

    /**
     * Returns the length of the value that is being masked.
     * For string values the length includes the opening and closing quotes.
     */
    int valueLength();

    /**
     * Indicates that value (or part of the value) needs to be replaced with a mask.
     *
     * <p> Note: the replacement might result in an invalid JSON, make sure to include opening and closing quotes when
     * the replacement is a string value.
     *
     * @param fromIndex  index from which the replacement should start. For string values the opening quote is included
     * @param length     length of the value to mask. relative to the fromIndex. For string values the closing quote is included
     * @param mask       the mask to replace the value with
     * @param maskRepeat number of times to repeat the mask, useful for masking digits or characters, for static masks the value should be 1.
     *
     * @see ValueMasker#maskWith(String) static string replacement imnplementation
     * @see ValueMasker#maskEmail(int, int, boolean, String) dynamic masking implementation
     */
    void replaceValue(int fromIndex, int length, byte[] mask, int maskRepeat);

    /**
     * Returns the amount of non-visible characters in the value.
     * @param fromIndex index from which the counting should start
     * @param length length of the value to count to, relative to the fromIndex
     * @return amount of non-visible characters in the value
     *
     * @see dev.blaauwendraad.masker.json.util.Utf8Util#countNonVisibleCharacters(byte[], int, int)
     */
    int countNonVisibleCharacters(int fromIndex, int length);

    /**
     * Returns the value as a string. For string values the opening and closing quotes are removed.
     */
    String asText();
}
