package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.util.Utf8Util;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the state of the {@link JsonMasker} at a given point in time during the {@link JsonMasker#mask(byte[])}
 * operation.
 */
class MaskingState implements ValueMaskerContext {
    protected byte[] message;
    protected int messageLength;
    protected int currentIndex = 0;
    private final List<ReplacementOperation> replacementOperations = new ArrayList<>();
    /**
     * The index marking the end of the last replacement operation. Used to determine the start point for the next
     * replacement operation to ensure they don't overlap.
     */
    protected int lastReplacementEndIndex = 0;
    /**
     * The total difference in the replacement operations related to the byte masking process.
     * This counter aggregates the difference in lengths between the original byte sequences and
     * their corresponding replacement masks over multiple replacement operations.
     * <p>
     * It is used to keep track of the cumulative change in byte count which informs buffer adjustments
     * during the masking process in order to accommodate the length variations due to replacements.
     */
    protected int replacementOperationsTotalDifference = 0;

    protected int currentTokenStartIndex = -1;

    private KeyMatcher.RadixTriePointer keyMatcherRootNodePointer;

    public MaskingState(byte[] message, KeyMatcher.RadixTriePointer keyMatcherRootNodePointer) {
        this.message = message;
        this.messageLength = message.length;
        this.keyMatcherRootNodePointer = keyMatcherRootNodePointer;
    }

    /**
     * Advances to the next byte in the message or (Streaming API) buffer, expanding the buffer if necessary.
     *
     * @return {@code true} if the current index is within the bounds of the message or if the (Streaming API) buffer
     * was successfully reloaded and more data is available in the stream, {@code false} otherwise
     */
    public boolean next() {
        return ++currentIndex < messageLength;
    }

    public void incrementIndex(int length) {
        currentIndex += length;
    }

    public byte byteAtCurrentIndex() {
        return message[currentIndex];
    }

    public boolean endOfJson() {
        return currentIndex >= messageLength;
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
     * {@link #flushReplacementOperations()}.
     *
     * @param startIndex the start index of the target value in the byte array
     * @param length     the length of the target value to be replaced
     * @param mask       the byte array representing the mask
     * @param maskRepeat the number of times the mask should be repeated
     * @throws UncheckedIOException if an I/O error occurs while writing to the output stream
     * @see ReplacementOperation
     */
    public void replaceTargetValueWith(int startIndex, int length, byte[] mask, int maskRepeat) {
        ReplacementOperation replacementOperation = new ReplacementOperation(startIndex, length, mask, maskRepeat);
        replacementOperations.add(replacementOperation);
        replacementOperationsTotalDifference += replacementOperation.difference();
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

    public KeyMatcher.RadixTriePointer getKeyMatcherRootNodePointer() {
        return keyMatcherRootNodePointer;
    }

    @Override
    public byte getByte(int index) {
        checkCurrentValueBounds(index);
        return message[getCurrentTokenStartIndex() + index];
    }

    @Override
    public int byteLength() {
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
        return new InvalidJsonException(String.format("%s at index %s", message, offset + index));
    }

    private void checkCurrentValueBounds(int index) {
        if (index < 0 || index >= byteLength()) {
            throw new IndexOutOfBoundsException("Index " + index + " is out of bounds for value of length " + byteLength());
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
     * @see #flushReplacementOperations()
     */
    private static final class ReplacementOperation {
        private final int startIndex;
        private final int length;
        private final byte[] mask;
        private final int maskRepeat;

        /**
         * @param startIndex index from which to start replacing
         * @param length     the length of the target value slice
         * @param mask       byte array mask to use as replacement for the value
         * @param maskRepeat number of times to repeat the mask (for cases when every character or digit is masked)
         */
        ReplacementOperation(int startIndex, int length, byte[] mask, int maskRepeat) {
            this.startIndex = startIndex;
            this.length = length;
            this.mask = mask;
            this.maskRepeat = maskRepeat;
        }

        /**
         * The difference between the mask length and the length of the target value to replace.
         * Used to calculate keep track of the offset during replacements.
         */
        int difference() {
            return mask.length * maskRepeat - length;
        }
    }
}
