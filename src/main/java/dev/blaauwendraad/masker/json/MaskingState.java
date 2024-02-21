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
    private final boolean trackJsonPath;

    /**
     * Current json path is represented by a dequeue of segment references.
     */
    private final Deque<SegmentReference> currentJsonPath = new ArrayDeque<>();

    public MaskingState(byte[] message, int currentIndex, boolean trackJsonPath) {
        this.message = message;
        this.currentIndex = currentIndex;
        this.trackJsonPath = trackJsonPath;
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
    public void addReplacementOperation(int startIndex, int endIndex, byte[] mask, int maskRepeat) {
        ReplacementOperation replacementOperation = new ReplacementOperation(startIndex, endIndex, mask, maskRepeat);
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
     * Expands current jsonpath with a new "key" segment.
     * @param start the index of a new segment start in <code>message</code>
     * @param offset the length of a new segment.
     */
    public void expandCurrentJsonPath(int start, int offset) {
        if (trackJsonPath) {
            currentJsonPath.push(new SegmentReference(start, offset));
        }
    }

    /**
     * Expands current jsonpath with a new array segment.
     */
    public void expandCurrentJsonPath() {
        if (trackJsonPath) {
            currentJsonPath.push(new SegmentReference(0, -1));
        }
    }

    /**
     * Backtracks current jsonpath to the previous segment.
     */
    public void backtrackCurrentJsonPath() {
        if (trackJsonPath) {
            currentJsonPath.pop();
        }
    }

    /**
     * Checks if the last segment of the current jsonpath is an array index.
     */
    public boolean isInArray() {
        if (!trackJsonPath) {
            return false;
        }
        return !currentJsonPath.isEmpty() && currentJsonPath.peek().offset == -1;
    }

    /**
     * Increments an array index in the last segment of the current jsonpath.
     * Throws {@link java.lang.IllegalStateException} if the last segment is not an array index.
     */
    public void incrementCurrentJsonPathArrayIndex() {
        if (trackJsonPath) {
            SegmentReference lastSegment = currentJsonPath.peek();
            assert isInArray() && lastSegment != null;
            lastSegment.start++;
        }
    }

    /**
     * Returns the iterator over the json path component references from head to tail
     */
    public Iterator<SegmentReference> getCurrentJsonPath() {
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
     * @param mask       byte array mask to use as replacement for the value
     * @param maskRepeat number of times to repeat the mask (for cases when every character or digit is masked)
     */
    @SuppressWarnings("java:S6218") // never used for comparison
    public record ReplacementOperation(int startIndex, int endIndex, byte[] mask, int maskRepeat) {

        /**
         * The difference between the mask length and the length of the target value to replace.
         * Used to calculate keep track of the offset during replacements.
         */
        public int difference() {
            return mask.length * maskRepeat - (endIndex - startIndex);
        }
    }

    /**
     * A mutable reference to a sequence of bytes in <code>message</code>. It is used to represent json path segments.
     * A "key" segment type reference is represented as a (start, offset) pair.
     * For an "array index" segment type reference, a (start, offset) pair is interpreted as (index, -1).
     */
    public static class SegmentReference {
        int start;
        int offset;

        SegmentReference(int start, int offset) {
            this.start = start;
            this.offset = offset;
        }
    }

}
