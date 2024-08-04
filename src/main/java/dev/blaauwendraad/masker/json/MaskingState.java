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
    private static final int INITIAL_JSONPATH_STACK_CAPACITY = 16; // an initial size of the jsonpath array
    private static final String STREAM_READ_ERROR_MESSAGE = "Failed to read from input stream";
    private static final String STREAM_WRITE_ERROR_MESSAGE = "Failed to write to output stream";

    private byte[] message;
    private int messageLength;
    private int bufferSize; // size of byte array buffers to be read from the input stream
    private int currentIndex = 0;
    private final List<ReplacementOperation> replacementOperations = new ArrayList<>();
    private int lastReplacementEndIndex = 0;
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
         character ("t", "f" and "n" respectively) and assume the identified value length. When masker is in allow mode,
         we may step over these values. In case the buffer size is less than the maximum possible length of such
         "special" value, we end up stepping over the entire buffer. To mitigate that, we force the minimum buffer size
         to be the maximum possible length, which is 5 (in "false").
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
     * @return true if jsonpath masking is enabled, false otherwise
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
     * Backtracks current jsonpath to the previous segment.
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

    boolean isCurrentTokenRegistered() {
        return currentTokenStartIndex != -1;
    }

    public int getCurrentTokenStartIndex() {
        if (!isCurrentTokenRegistered()) {
            throw new IllegalStateException("No current value index set to mask");
        }
        return currentTokenStartIndex;
    }

    /**
     * Register the current index as the start index of the token. The token could be either a key or a value of type
     * "string", "number", "boolean" or null.
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
     * Flushes the current buffer into provided OutputStream, then moves the current token to the beginning of the buffer
     * and fills up the buffer from provided InputStream.
     * In case the current token is too long (the start index is not in the last quarter of the buffer),
     * extends the current buffer by 2.
     *
     * @return true if more data is available in the input stream, false if the end of the stream is reached.
     */
    boolean reloadBuffer() {
        flushCurrentBuffer();
        return readNextBuffer();
    }

    /**
     * Flushes the remaining of the current buffer up to the current token start index into output stream
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
     * @return true if more data is available in the stream, false otherwise
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
     * Moves the current token to the beginning of buffer. If current token size is larger than a quarter of the buffer size,
     * extends the buffer size by 2
     */
    private void moveCurrentTokenToBeginningOfBuffer(int currentTokenLength) {
        if (currentTokenLength < bufferSize >> 2) {
            // in case the current value is shorter than a quarter of the buffer,
            // fill up the buffer without extending its length

            // move the current value to the beginning of the buffer
            System.arraycopy(message, currentTokenStartIndex, message, 0, currentTokenLength);
        } else {
            // in case the current value is longer than a quarter of the buffer,
            // extend the buffer length by a quarter
            bufferSize <<= 1;
            if (bufferSize >= 65536) {
                throw new IllegalStateException("Buffer overflow");
            }
            byte[] extension = new byte[bufferSize];

            // move the current value to the beginning of the extension
            System.arraycopy(message, currentTokenStartIndex, extension, 0, currentTokenLength);
            message = extension;
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
