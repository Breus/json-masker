package dev.blaauwendraad.masker.json;

/**
 * A mutable reference to a sequence of bytes in <code>dev.blaauwendraad.masker.json.MaskingState#message</code>.
 * It is used to represent json path segments.
 * <p>
 * There are two types of segment references:
 * <ul>
 *     <li>{@link Node} - a reference to a node in a json path, where <code>offset</code> denotes the start index of a
 *     segment in the message and <code>length</code> denotes the length of a segment in the message</li>
 *     <li>{@link Array} - a reference to an array in a json path, where <code>index</code> denotes the element index.</li>
 * </ul>
 */
sealed interface JsonPathSegmentReference permits JsonPathSegmentReference.Array, JsonPathSegmentReference.Node {
    final class Node implements JsonPathSegmentReference {
        private final int offset;
        private final int length;

        Node(int index, int length) {
            this.offset = index;
            this.length = length;
        }

        public int getOffset() {
            return this.offset;
        }

        public int getLength() {
            return this.length;
        }
    }

    final class Array implements JsonPathSegmentReference {
        private int index;

        public Array(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public void increment() {
            this.index++;
        }
    }
}
