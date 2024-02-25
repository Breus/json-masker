package dev.blaauwendraad.masker.json;

/**
 * A mutable reference to a sequence of bytes in <code>dev.blaauwendraad.masker.json.MaskingState#message</code>. It is used to represent json path segments.
 * <p>
 * A reference is represented by a pair of integers:
 * <ul>
 *  <li><code>offset</code> denotes the start index of a segment in the message. In case the segment is an array, <code>offset</code> denotes the element index.</li>
 *  <li><code>length</code> denotes the length of a segment in the message. In case the segment is an array, <code>length</code> is set to -1.</li>
 * </ul>
 */
public class JsonPathSegmentReference {
    private int offset;
    private int length;

    JsonPathSegmentReference(int index, int length) {
        this.offset = index;
        this.length = length;
    }

    JsonPathSegmentReference(int index) {
        this.offset = index;
        this.length = -1;
    }

    public int getOffset() {
        return this.offset;
    }

    public int getLength() {
        return this.length;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setLength(int length) {
        this.length = length;
    }

    /**
     * <code>length</code> is assumed to be set to -1 for array segments.
     */
    public boolean isArraySegment() {
        return this.length == -1;
    }
}
