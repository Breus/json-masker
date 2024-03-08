package dev.blaauwendraad.masker.json;

import javax.annotation.CheckForNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the state of the {@link JsonMasker} at a given point in time during the {@link JsonMasker#mask(byte[])}
 * operation.
 */
public final class MaskingState {
    private final byte[] message;
    private int currentIndex = 0;
    private final List<ReplacementOperation> replacementOperations = new ArrayList<>();
    private int replacementOperationsTotalDifference = 0;

    /**
     * Current json path is represented by a dequeue of segment references.
     */
    private final JsonPathNode[] currentJsonPath;
    private int currentJsonPathIndex = -1;

    public MaskingState(byte[] message, boolean trackJsonPath) {
        this.message = message;
        if (trackJsonPath) {
            currentJsonPath = new JsonPathNode[100];
        } else {
            currentJsonPath = null;
        }
    }

    public void incrementCurrentIndex() {
        currentIndex++;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
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
     * Checks if jsonpath masking is enabled.
     * @return true if jsonpath masking is enabled, false otherwise
     */
    boolean jsonPathEnabled() {
        return currentJsonPath != null;
    }

    /**
     * Expands current jsonpath with a new "key" segment.
     * @param start the index of a new segment start in <code>message</code>
     * @param offset the length of a new segment.
     */
    void expandCurrentJsonPath(int start, int offset) {
        if (currentJsonPath != null) {
            currentJsonPath[++currentJsonPathIndex] = new JsonPathNode.Node(start, offset);
        }
    }

    /**
     * Expands current jsonpath with a new array segment.
     */
    void expandCurrentJsonPathWithArray() {
        if (currentJsonPath != null) {
            currentJsonPath[++currentJsonPathIndex] = new JsonPathNode.Array();
        }
    }

    /**
     * Backtracks current jsonpath to the previous segment.
     */
    void backtrackCurrentJsonPath() {
        if (currentJsonPath != null) {
            currentJsonPath[currentJsonPathIndex--] = null;
        }
    }

    /**
     * Returns the iterator over the json path component references from head to tail
     */
    @CheckForNull
    JsonPathNode[] getCurrentJsonPath() {
        return currentJsonPath;
    }

    int getCurrentJsonPathIndex() {
        return currentJsonPathIndex;
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

}
