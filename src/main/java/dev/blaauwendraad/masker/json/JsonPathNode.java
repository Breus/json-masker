package dev.blaauwendraad.masker.json;

/**
 * A mutable reference to a sequence of bytes in <code>dev.blaauwendraad.masker.json.MaskingState#message</code>.
 * It is used to represent json path nodes.
 * <p>
 * There are two types of nodes:
 * <ul>
 *     <li>{@link Node} - a reference to a node in a json path, where <code>offset</code> denotes the start index of a
 *     segment in the message and <code>length</code> denotes the length of a segment in the message</li>
 *     <li>{@link Array} - a reference to an array in a json path. Only wildcard indexes are supported.</li>
 * </ul>
 */
sealed interface JsonPathNode permits JsonPathNode.Array, JsonPathNode.Node {
    final class Node implements JsonPathNode {
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

    final class Array implements JsonPathNode {
        // only wildcard indexes are supported
    }
}
