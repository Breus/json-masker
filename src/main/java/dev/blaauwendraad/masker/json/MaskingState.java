package dev.blaauwendraad.masker.json;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Represents the state of the {@link JsonMasker} at a given point in time during the {@link JsonMasker#mask(byte[])}
 * operation.
 */
public final class MaskingState {
    private byte[] message;
    private int currentIndex;
    private final List<ReplacementOperation> replacementOperations = new ArrayList<>();
    private int replacementOperationsTotalDifference = 0;

    /**
     * Current json path is represented by a dequeue of pairs of integers.
     * A pair is interpreted as (keyStartIndex, keyLength), where "keyStartIndex" is the index of the key start in
     * message byte array, and "keyLength" is the length of the key.
     */
    private final Deque<int[]> currentJsonPath = new ArrayDeque<>();

    public MaskingState(byte[] message, int currentIndex) {
        this.message = message;
        this.currentIndex = currentIndex;
    }

    public void incrementCurrentIndex() {
        currentIndex++;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public void setMessage(byte[] newMessage) {
        this.message = newMessage;
    }

    public byte byteAtCurrentIndex() {
        return message[currentIndex];
    }

    public byte byteAtCurrentIndexMinusOne() {
        return message[currentIndex - 1];
    }

    public int currentIndex() {
        return currentIndex;
    }

    public int messageLength() {
        return message.length;
    }

    public byte[] getMessage() {
        return message;
    }

    /**
     * Adds new delayed replacement operation to the list of operations to be applied to the message.
     */
    public void addReplacementOperation(int startIndex, int endIndex, int maskLength, byte maskByte) {
        ReplacementOperation replacementOperation = new ReplacementOperation(startIndex, endIndex, maskLength, maskByte);
        replacementOperations.add(replacementOperation);
        replacementOperationsTotalDifference += replacementOperation.difference();
    }

    /**
     * Returns the list of replacement operations that need to be applied to the message.
     */
    public List<ReplacementOperation> getReplacementOperations() {
        return replacementOperations;
    }

    /**
     * Returns the total difference between the masks and target values lengths of all replacement operations.
     */
    public int getReplacementOperationsTotalDifference() {
        return replacementOperationsTotalDifference;
    }


    /**
     * Expands current json path with the key reference
     */
    public void expandCurrentJsonPath(int keyStartIndex, int keyLength) {
        currentJsonPath.push(new int[]{keyStartIndex, keyLength});
    }

    /**
     * Backtracks to the previous json path component
     */
    public void backtrackCurrentJsonPath() {
        currentJsonPath.pop();
    }

    /**
     * Returns the iterator over the json path component references from head to tail
     */
    public Iterator<int[]> getCurrentJsonPath() {
        return currentJsonPath.descendingIterator();
    }

    // for debugging purposes, shows the current state of message traversal
    @Override
    public String toString() {
        return "current: '" + (currentIndex == message.length ? "<end of json>" : (char) message[currentIndex]) + "'," +
                " before: '" + new String(message, Math.max(0, currentIndex - 10), Math.min(10, currentIndex)) + "'," +
                " after: '" + new String(message, currentIndex, Math.min(10, message.length - currentIndex)) + "'";
    }

    /**
     * Represents a delayed replacement that requires resizing of the message byte array. In order to avoid resizing on
     * every mask, we store the replacement operations in a list and apply them all at once at the end, thus making only
     * a single resize operation.
     *
     * @param startIndex index from which to start replacing
     * @param endIndex   index at which to stop replacing
     * @param maskLength length of the mask to apply
     * @param maskByte   byte to use for the mask
     */
    public record ReplacementOperation(int startIndex, int endIndex, int maskLength, byte maskByte) {

        /**
         * The difference between the mask length and the length of the target value to replace.
         * Used to calculate keep track of the offset during replacements.
         */
        public int difference() {
            return maskLength - (endIndex - startIndex);
        }
    }
}
