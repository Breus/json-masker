package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.util.Utf8Util;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the state of the {@link JsonMasker} at a given point in time during the {@link JsonMasker#mask(byte[])}
 * operation.
 */
final class MaskingState implements ValueMaskerContext {
    private final byte[] message;
    private int currentIndex = 0;
    private final List<ReplacementOperation> replacementOperations = new ArrayList<>();
    private int replacementOperationsTotalDifference = 0;

    /**
     * A reference to the end of the current JsonPATH segment in the JsonPATH trie.
     */
    private KeyMatcher.TrieNode.@Nullable JsonPathTrieNode currentJsonPathHead = null;

    /**
     * A "null pit" here is a situation when {@code currentJsonPathNode} reaches NULL because the current segment is not in the trie and keeps expanding.
     * We need to keep track of how deep we are in the pit so that we know when we are out of it and can continue traversing the trie
     */
    private int nullPitDepth = 0;

    private int currentValueStartIndex = -1;

    public MaskingState(byte[] message) {
        this.message = message;
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
        return currentIndex == message.length;
    }

    public int currentIndex() {
        return currentIndex;
    }

    public byte[] getMessage() {
        return message;
    }

    public KeyMatcher.TrieNode.@Nullable JsonPathTrieNode getCurrentJsonPathHead() {
        if (nullPitDepth > 0) {
            return null;
        }
        return this.currentJsonPathHead;
    }

    /**
     * Moves current JsonPATH head to a new node {@code newJsonPathHead}.
     * If the new head is null or is not a complete segment, then the masker assumes it is in a "NULL pit".
     * A "NULL pit" here is a situation when the current JsonPATH is not found in the trie, but keeps expanding.
     *
     * @param newJsonPathHead a new JsonPATH head.
     */
    public void moveCurrentJsonPathHead(KeyMatcher.TrieNode.@Nullable JsonPathTrieNode newJsonPathHead) {
        if (newJsonPathHead == null || !newJsonPathHead.isEndOfSegment()) {
            nullPitDepth++;
        } else {
            this.currentJsonPathHead = newJsonPathHead;
        }
    }

    /**
     * Backtracks the current JsonPATH head to the end of the parent segment.
     * In case the masker is in a "NULL pit", then decrease the depth of the pit.
     * A "NULL pit" here is a situation when the current JsonPATH is not found in the trie, but keeps expanding.
     */
    public void backtrackCurrentJsonPath() {
        if (nullPitDepth > 0) {
            nullPitDepth--;
        } else if (currentJsonPathHead != null) {
            currentJsonPathHead = currentJsonPathHead.endOfParentSegment;
        }
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
        return currentIndex - getCurrentValueStartIndex();
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
        if (currentIndex == message.length) {
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
