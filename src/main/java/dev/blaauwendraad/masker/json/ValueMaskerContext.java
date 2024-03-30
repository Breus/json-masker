package dev.blaauwendraad.masker.json;

/**
 * Represents the original value that is currently being masked. The context is passed to {@link
 * ValueMasker#maskValue(ValueMaskerContext)} to allow the implementation to replace the JSON value
 * with a custom mask.
 *
 * <p> The JSON value is represented by a byte array to provide a way to access the content of the
 * original value, based on index. The index includes the JSON value in its entirety which means
 * that for example for strings the opening and closing quotes are included. The reason for this is
 * that the API becomes more flexible as it enables strings to be replaced by {@code null}, for
 * example.
 */
public interface ValueMaskerContext {
    /**
     * Retrieve the byte at the given index in the original value that is being masked.
     *
     * @param index the index in the original value between {@code 0} (inclusive) and {@link
     *     #byteLength()} (exclusive)
     */
    byte getByte(int index);

    /** Returns the length of the original value that is being masked in bytes. */
    int byteLength();

    /**
     * Indicates that the bytes of the original value (or part of the value) needs to be replaced
     * with a mask.
     *
     * <p>Note: the replacement might result in an invalid JSON, make sure to include opening and
     * closing quotes when the replacement is a string value.
     *
     * @param fromIndex index from which the replacement should start. For string values, the
     *     opening quote is included
     * @param length length of the value to mask. relative to the fromIndex. For string values the
     *     closing quote is included
     * @param mask the mask to replace the original value with
     * @param maskRepeat number of times to repeat the mask, useful for masking digits or
     *     characters, for static masks the value should be 1.
     * @see ValueMaskers#with(String) static string replacement imnplementation
     * @see ValueMaskers#email(int, int, boolean, String) dynamic masking implementation
     */
    void replaceBytes(int fromIndex, int length, byte[] mask, int maskRepeat);

    /**
     * Returns the number of non-visible, human-readable characters in the original value.
     *
     * @param fromIndex index from which the counting should start
     * @param length length of the value to count to, relative to the fromIndex
     * @return number of non-visible characters in the value
     * @see dev.blaauwendraad.masker.json.util.Utf8Util#countNonVisibleCharacters(byte[], int, int)
     */
    int countNonVisibleCharacters(int fromIndex, int length);

    /**
     * Returns a string representation of the original JSON value. 
     * <p>
     * Note: this INCLUDES the opening and closing quotes for string values 
     */
    String asString(int fromIndex, int length);

    /**
     * Create an {@link InvalidJsonException} with the given message and index relative to the value (i.e. an index
     * between {@code 0} and {@link ValueMaskerContext#byteLength()}).
     *
     * @param message error message
     * @param index relative index where the JSON contains invalid sequence
     * @return the exception to be thrown
     */
    InvalidJsonException invalidJson(String message, int index);
}
