package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.util.Utf8Util;
import org.jspecify.annotations.Nullable;

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
    private final byte[] message;
    private int currentIndex = 0;
    private final List<ReplacementOperation> replacementOperations = new ArrayList<>();
    private int replacementOperationsTotalDifference = 0;

    /**
     * Current JSONPath is represented by a stack of segment references.
     * A stack is implemented with an array of the trie nodes that reference the end of the segment
     */
    private KeyMatcher.@Nullable TrieNode @Nullable [] currentJsonPath = null;
    private int currentJsonPathHeadIndex = -1;
    private int currentValueStartIndex = -1;

    public MaskingState(byte[] message, boolean trackJsonPath) {
        this.message = message;
        if (trackJsonPath) {
            currentJsonPath = new KeyMatcher.TrieNode[INITIAL_JSONPATH_STACK_CAPACITY];
        }
    }

    public boolean next() {
        return ++currentIndex < message.length;
    }

    public void incrementIndex(int length) {
        currentIndex += length;
    }

    public byte byteAtCurrentIndex() {
        return message[currentIndex];
    }

    public boolean endOfJson() {
        return currentIndex >= message.length;
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
        byte[] newMessage = new byte[message.length + replacementOperationsTotalDifference];

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
                message.length - index
        );

        // make sure no operations are performed after this
        this.currentIndex = Integer.MAX_VALUE;

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

    public int getCurrentValueStartIndex() {
        if (currentValueStartIndex == -1) {
            throw new IllegalStateException("No current value index set to mask");
        }
        return currentValueStartIndex;
    }

    /**
     * Register the current index as the start index of the value to be masked.
     */
    public void registerValueStartIndex() {
        this.currentValueStartIndex = currentIndex;
    }

    /**
     * Clears the previous registered value start index.
     */
    public void clearValueStartIndex() {
        this.currentValueStartIndex = -1;
    }

    @Override
    public byte getByte(int index) {
        checkCurrentValueBounds(index);
        return message[getCurrentValueStartIndex() + index];
    }

    @Override
    public int byteLength() {
        return Math.min(currentIndex, message.length) - getCurrentValueStartIndex();
    }

    @Override
    public void replaceBytes(int fromIndex, int length, byte[] mask, int maskRepeat) {
        checkCurrentValueBounds(fromIndex);
        checkCurrentValueBounds(fromIndex + length - 1);
        replaceTargetValueWith(getCurrentValueStartIndex() + fromIndex, length, mask, maskRepeat);
    }

    @Override
    public int countNonVisibleCharacters(int fromIndex, int length) {
        checkCurrentValueBounds(fromIndex);
        checkCurrentValueBounds(fromIndex + length - 1);
        return Utf8Util.countNonVisibleCharacters(
                message,
                getCurrentValueStartIndex() + fromIndex,
                length
        );
    }

    @Override
    public String asString(int fromIndex, int length) {
        checkCurrentValueBounds(fromIndex);
        checkCurrentValueBounds(fromIndex + length - 1);
        int offset = getCurrentValueStartIndex();
        return new String(message, offset + fromIndex, length, StandardCharsets.UTF_8);
    }

    @Override
    public InvalidJsonException invalidJson(String message, int index) {
        int offset = getCurrentValueStartIndex();
        return new InvalidJsonException("%s at index %s".formatted(message, offset + index));
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
        sb.append(new String(message, Math.max(0, currentIndex - 10), Math.min(10, currentIndex)));
        sb.append(">");
        if (endOfJson()) {
            sb.append("<end of json>");
        } else {
            sb.append((char) message[currentIndex]);
            if (currentIndex + 1 < message.length) {
                sb.append("<");
                sb.append(new String(message, currentIndex + 1, Math.min(10, message.length - currentIndex - 1)));
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
