package dev.blaauwendraad.masker.json;
import dev.blaauwendraad.masker.json.util.Utf8Util;
import org.jspecify.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents the state of the {@link JsonMasker} at a given point in time during the {@link JsonMasker#mask(byte[])}
 * operation.
 */
final class MaskingState implements ValueMaskerContext {
    private static final int INITIAL_JSONPATH_STACK_CAPACITY = 16; // an initial size of the JsonPath array
    /**
     * Defines the maximum size for the buffer used by the streaming API:
     * {@link JsonMasker#mask(InputStream, OutputStream)}.
     * <p>
     * This is a security measure to prevent too much memory being allocated for maliciously crafted JSONs with huge
     * tokens (keys or values) to consume too much memory.
     * <p>
     * The maximum allowed buffer size corresponds to 16MB, which corresponds to a maximum token length of 4 million
     * characters.
     */
    private static final int MAX_BUFFER_SIZE = 16777216;
    private static final String STREAM_READ_ERROR_MESSAGE = "Failed to read from input stream";
    private static final String STREAM_WRITE_ERROR_MESSAGE = "Failed to write to output stream";

    private byte[] message;
    private int messageLength;
    private int bufferSize; // size of byte array buffers to be read from the input stream
    private int currentIndex = 0;
    private final List<ReplacementOperation> replacementOperations = new ArrayList<>();
    /**
     * The index marking the end of the last replacement operation. Used to determine the start point for the next
     * replacement operation to ensure they don't overlap.
     */
    private int lastReplacementEndIndex = 0;
    /**
     * The total difference in the replacement operations related to the byte masking process.
     * This counter aggregates the difference in lengths between the original byte sequences and
     * their corresponding replacement masks over multiple replacement operations.
     * <p>
     * It is used to keep track of the cumulative change in byte count which informs buffer adjustments
     * during the masking process in order to accommodate the length variations due to replacements.
     */
    private int replacementOperationsTotalDifference = 0;
    @Nullable private final InputStream inputStream;
    @Nullable private final OutputStream outputStream;

    /**
     * Current JSONPath is represented by a stack of segment references.
     * A stack is implemented with an array of the trie nodes that reference the end of the segment
     */
    private KeyMatcher.@Nullable TrieNode @Nullable [] currentJsonPath = null;
    private int currentJsonPathHeadIndex = -1;
    private int currentTokenStartIndex = -1;

    public MaskingState(byte[] message, boolean trackJsonPath) {
        this.message = message;
        this.messageLength = message.length;
        if (trackJsonPath) {
            currentJsonPath = new KeyMatcher.TrieNode[INITIAL_JSONPATH_STACK_CAPACITY];
        }
        this.inputStream = null;
        this.outputStream = null;
    }

    public MaskingState(InputStream inputStream, OutputStream outputStream, boolean trackJsonPath, int bufferSize) {
        /*
         There is a special optimization for "true", "false" and "null" values. We identify such values by their first
         character ("t", "f" and "n" respectively) and assume the identified value length. When the masker is in allow
         mode, we may step over these values. In case the buffer size is less than the maximum possible length of such a
         "special" value, we end up stepping over the entire buffer. To mitigate that, we force the minimum buffer size
         to be the maximum possible length of such "special" values, which is 5 (in "false").
        */
        if (bufferSize < 5) {
            throw new IllegalArgumentException("Buffer size must be at least 5 bytes");
        }
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.bufferSize = bufferSize;
        this.message = new byte[this.bufferSize];
        this.messageLength = 0;
        if (trackJsonPath) {
            currentJsonPath = new KeyMatcher.TrieNode[INITIAL_JSONPATH_STACK_CAPACITY];
        }
        readNextBuffer();
    }

    /**
     * Advances to the next byte in the message or (Streaming API) buffer, expanding the buffer if necessary.
     *
     * @return {@code true} if the current index is within the bounds of the message or if the (Streaming API) buffer
     * was successfully reloaded and more data is available in the stream, {@code false} otherwise
     */
    public boolean next() {
        return ++currentIndex < messageLength || reloadBuffer();
    }

    public void incrementIndex(int length) {
        currentIndex += length;
    }

    public byte byteAtCurrentIndex() {
        return message[currentIndex];
    }

    public boolean endOfJson() {
        return currentIndex >= messageLength && !reloadBuffer();
    }

    public int currentIndex() {
        return currentIndex;
    }

    public byte[] getMessage() {
        return message;
    }

    /**
     * Replaces a target value (byte slice) with a mask byte. If lengths of both target value and mask are equal, the
     * replacement is done in-place, otherwise a replacement operation is recorded to be performed as a batch using
     * {@link #flushReplacementOperations}.
     *
     * @param startIndex the start index of the target value in the byte array
     * @param length     the length of the target value to be replaced
     * @param mask       the byte array representing the mask
     * @param maskRepeat the number of times the mask should be repeated
     * @throws UncheckedIOException if an I/O error occurs while writing to the output stream
     * @see ReplacementOperation
     */
    public void replaceTargetValueWith(int startIndex, int length, byte[] mask, int maskRepeat) {
        if (outputStream == null) {
            ReplacementOperation replacementOperation = new ReplacementOperation(startIndex, length, mask, maskRepeat);
            replacementOperations.add(replacementOperation);
            replacementOperationsTotalDifference += replacementOperation.difference();
        } else {
            // write the replacement into the output stream
            try {
                // write everything up to the beginning of the current replacement
                outputStream.write(message, lastReplacementEndIndex, startIndex - lastReplacementEndIndex);

                // write the replacement
                for (int i = 0; i < maskRepeat; i++) {
                    outputStream.write(mask);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(STREAM_WRITE_ERROR_MESSAGE, e);
            }
            lastReplacementEndIndex = startIndex + length;
        }
    }

    /**
     * Performs all replacement operations to the message array, must be called at the end of the replacements.
     * <p>
     * For every operation that required resizing of the original array, to avoid copying the array multiple times,
     * those operations were stored in a list and can be performed in one go, thus resizing the array only once.
     * <p>
     * Replacement operation is only recorded if the length of the target value is different from the length of the mask,
     * otherwise the replacement must have been done in-place.
     *
     * @return the message array with all replacement operations performed.
     */
    public byte[] flushReplacementOperations() {
        if (replacementOperations.isEmpty()) {
            return message;
        }

        // Create new empty array with a length computed by the difference of all mismatches of lengths between the target values and the masks
        // in some edge cases the length difference might be equal to 0, but since some indices mismatch (otherwise there would be no replacement operations)
        // we still have to copy the array to keep track of data according to original indices
        byte[] newMessage = new byte[messageLength + replacementOperationsTotalDifference];

        // Index of the original message array
        int index = 0;
        // Offset is the difference between the original and new array indices, we need it to calculate indices
        // in the new message array using startIndex and endIndex, which are indices in the original array
        int offset = 0;
        for (ReplacementOperation replacementOperation : replacementOperations) {
            // Copy everything from message up until replacement operation start index
            System.arraycopy(
                    message,
                    index,
                    newMessage,
                    index + offset,
                    replacementOperation.startIndex - index
            );
            // Insert the mask bytes
            int length = replacementOperation.mask.length;
            for (int i = 0; i < replacementOperation.maskRepeat; i++) {
                System.arraycopy(
                        replacementOperation.mask,
                        0,
                        newMessage,
                        replacementOperation.startIndex + offset + i * length,
                        length
                );
            }
            // Adjust index and offset to continue copying from the end of the replacement operation
            index = replacementOperation.startIndex + replacementOperation.length;
            offset += replacementOperation.difference();
        }

        // Copy the remainder of the original array
        System.arraycopy(
                message,
                index,
                newMessage,
                index + offset,
                messageLength - index
        );

        return newMessage;
    }

    /**
     * Checks if jsonpath masking is enabled.
     * @return {@code true} if JSONPath masking is enabled, {@code false} otherwise
     */
    boolean jsonPathEnabled() {
        return currentJsonPath != null;
    }

    /**
     * Expands current jsonpath.
     *
     * @param trieNode a node in the trie where the new segment ends.
     */
    void expandCurrentJsonPath(KeyMatcher.@Nullable TrieNode trieNode) {
        if (currentJsonPath != null) {
            currentJsonPath[++currentJsonPathHeadIndex] = trieNode;
            if (currentJsonPathHeadIndex == currentJsonPath.length - 1) {
                // resize
                currentJsonPath = Arrays.copyOf(currentJsonPath, currentJsonPath.length*2);
            }
        }
    }

    /**
     * Backtracks current JSONPath to the previous segment.
     */
    void backtrackCurrentJsonPath() {
        if (currentJsonPath != null) {
            currentJsonPath[currentJsonPathHeadIndex--] = null;
        }
    }

    /**
     * Returns the TrieNode that references the end of the latest segment in the current jsonpath
     */
    public KeyMatcher.@Nullable TrieNode getCurrentJsonPathNode() {
        if (currentJsonPath != null && currentJsonPathHeadIndex != -1) {
            return currentJsonPath[currentJsonPathHeadIndex];
        } else {
            return null;
        }
    }

    /**
     * Checks if the current token is registered within the masking state.
     *
     * @return {@code true} if the current token start index is registered (not -1), {@code false} otherwise
     */
    boolean isCurrentTokenRegistered() {
        return currentTokenStartIndex != -1;
    }

    /**
     * Returns the start index of the current token in the masking state.
     *
     * @return the start index of the current token
     * @throws IllegalStateException if no current token is registered
     */
    public int getCurrentTokenStartIndex() {
        if (!isCurrentTokenRegistered()) {
            throw new IllegalStateException("No current value index set to mask");
        }
        return currentTokenStartIndex;
    }

    /**
     * Register the current index as the start index of the token. The token could be either a JSON key or a value of
     * type "string", "number", "boolean" or null.
     */
    public void registerTokenStartIndex() {
        this.currentTokenStartIndex = currentIndex;
    }

    /**
     * Clears the previous registered token start index.
     */
    public void clearTokenStartIndex() {
        this.currentTokenStartIndex = -1;
    }

    @Override
    public byte getByte(int index) {
        checkCurrentValueBounds(index);
        return message[getCurrentTokenStartIndex() + index];
    }

    @Override
    public int byteLength() {
        if (messageLength <= currentIndex) {
            reloadBuffer();
        }
        return Math.min(currentIndex, messageLength) - getCurrentTokenStartIndex();
    }

    @Override
    public void replaceBytes(int fromIndex, int length, byte[] mask, int maskRepeat) {
        checkCurrentValueBounds(fromIndex);
        checkCurrentValueBounds(fromIndex + length - 1);
        replaceTargetValueWith(getCurrentTokenStartIndex() + fromIndex, length, mask, maskRepeat);
    }

    @Override
    public int countNonVisibleCharacters(int fromIndex, int length) {
        checkCurrentValueBounds(fromIndex);
        checkCurrentValueBounds(fromIndex + length - 1);
        return Utf8Util.countNonVisibleCharacters(
                message,
                getCurrentTokenStartIndex() + fromIndex,
                length
        );
    }

    @Override
    public String asString(int fromIndex, int length) {
        checkCurrentValueBounds(fromIndex);
        checkCurrentValueBounds(fromIndex + length - 1);
        int offset = getCurrentTokenStartIndex();
        return new String(message, offset + fromIndex, length, StandardCharsets.UTF_8);
    }

    @Override
    public InvalidJsonException invalidJson(String message, int index) {
        int offset = getCurrentTokenStartIndex();
        return new InvalidJsonException("%s at index %s".formatted(message, offset + index));
    }

    private void checkCurrentValueBounds(int index) {
        if (index < 0 || index >= byteLength()) {
            throw new IndexOutOfBoundsException("Index " + index + " is out of bounds for value of length " + byteLength());
        }
    }

    /**
     * Flushes the current buffer into the output stream, moves the current token to the beginning of the
     * buffer, and fills up the buffer from the input stream.
     * In case the current token is too long (i.e. the start index is not in the last quarter of the buffer),
     * double the current buffer size.
     *
     * @return {@code true} if more data is available in the input stream, {@code false} if the end of the
     * stream is reached.
     */
    boolean reloadBuffer() {
        flushCurrentBuffer();
        return readNextBuffer();
    }

    /**
     * Flushes the remaining of the current buffer up to the current token start index into the output stream
     *
     * @throws UncheckedIOException if an I/O error occurs while writing to the output stream
     */
    void flushCurrentBuffer() {
        if (outputStream == null) {
            return;
        }
        try {
            int remainingBufferLength = !isCurrentTokenRegistered() ?
                    messageLength - lastReplacementEndIndex : // flush the remaining of the message
                    currentTokenStartIndex - lastReplacementEndIndex; // flush the remaining of the message up to the current token start index
            outputStream.write(message, lastReplacementEndIndex, remainingBufferLength);
            outputStream.flush();
            lastReplacementEndIndex = 0;
        } catch (IOException e) {
            throw new UncheckedIOException(STREAM_READ_ERROR_MESSAGE, e);
        }
    }

    /**
     * Reads the next buffer and extends the buffer size if necessary.
     *
     * @throws UncheckedIOException if an I/O error occurs while reading from the input stream
     * @return {@code true} if more data is available in the stream, {@code false} otherwise
     */
    private boolean readNextBuffer() {
        if (inputStream == null) {
            return false;
        }
        if (!isCurrentTokenRegistered()) {
            // the pointer is not at a json value, so we are safe to read the next buffer
            currentIndex -= messageLength;
            try {
                messageLength = inputStream.readNBytes(message, 0, bufferSize);
            } catch (IOException e) {
                throw new UncheckedIOException(STREAM_READ_ERROR_MESSAGE, e);
            }
        } else {
            // the current buffer has ended before the masker finished processing the current value.
            int currentTokenLength = messageLength - currentTokenStartIndex;
            moveCurrentTokenToBeginningOfBuffer(currentTokenLength);

            // fill up the remaining of the buffer
            try {
                messageLength = inputStream.readNBytes(message, currentTokenLength, bufferSize - currentTokenLength) + currentTokenLength;
            } catch (IOException e) {
                throw new UncheckedIOException(STREAM_READ_ERROR_MESSAGE, e);
            }

            // reset pointers
            currentIndex -= currentTokenStartIndex;
            currentTokenStartIndex = 0;
        }
        return messageLength > currentIndex;
    }

    /**
     * Moves the current JSON token to the beginning of buffer.
     * <p>
     * If the current JSON token size is larger than a quarter of the buffer size, double the buffer size.
     *
     * @param currentTokenLength the length of the current JSON token, in bytes
     */
    private void moveCurrentTokenToBeginningOfBuffer(int currentTokenLength) {
        if (currentTokenLength < bufferSize >> 2) { // note: >> 2 is equal to dividing by 4
            // in case the current value is shorter than a quarter of the buffer fill up the buffer without extending its
            // length by moving the current value to the beginning of the buffer
            System.arraycopy(message, currentTokenStartIndex, message, 0, currentTokenLength);
        } else {
            // in case the current value is longer than a quarter of the buffer, double the buffer size
            bufferSize <<= 1; // note: <<= 1 is equal to doubling the bufferSize
            if (bufferSize > MAX_BUFFER_SIZE) {
                throw new InvalidJsonException(
                        "Invalid JSON input provided: it contains a single JSON token (key or value) with %s characters".formatted(
                                currentTokenLength));
            }
            byte[] extendedBuffer = new byte[bufferSize];

            // move the current value to the beginning of the extended buffer
            System.arraycopy(message, currentTokenStartIndex, extendedBuffer, 0, currentTokenLength);
            message = extendedBuffer;
        }
    }

    // for debugging purposes, shows the current state of message traversal
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(new String(message, Math.max(0, currentIndex - 10), Math.min(10, currentIndex), StandardCharsets.UTF_8));
        sb.append(">");
        if (currentIndex >= messageLength) {
            sb.append("<end of buffer>");
        } else {
            sb.append((char) message[currentIndex]);
            if (currentIndex + 1 < messageLength) {
                sb.append("<");
                sb.append(new String(message, currentIndex + 1, Math.min(10, messageLength - currentIndex - 1), StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }

    /**
     * Represents a delayed replacement that requires resizing of the message byte array. In order to avoid resizing on
     * every mask, we store the replacement operations in a list and apply them all at once at the end, thus making only
     * a single resize operation.
     *
     * @param startIndex index from which to start replacing
     * @param length     the length of the target value slice
     * @param mask       byte array mask to use as replacement for the value
     * @param maskRepeat number of times to repeat the mask (for cases when every character or digit is masked)
     *
     * @see #flushReplacementOperations()
     */
    @SuppressWarnings("java:S6218") // never used for comparison
    private record ReplacementOperation(int startIndex, int length, byte[] mask, int maskRepeat) {

        /**
         * The difference between the mask length and the length of the target value to replace.
         * Used to calculate keep track of the offset during replacements.
         */
        public int difference() {
            return mask.length * maskRepeat - length;
        }
    }

}
