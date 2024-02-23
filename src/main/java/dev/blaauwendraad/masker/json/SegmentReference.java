package dev.blaauwendraad.masker.json;

/**
 * A mutable reference to a sequence of bytes in <code>message</code>. It is used to represent json path segments.
 * A "key" segment type reference is represented as a (start, offset) pair.
 * For an "array index" segment type reference, a (start, offset) pair is interpreted as (index, -1).
 */
public class SegmentReference {
    int start;
    int offset;

    SegmentReference(int start, int offset) {
        this.start = start;
        this.offset = offset;
    }

    public boolean isArraySegment() {
        return this.offset == -1;
    }
}
