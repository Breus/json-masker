package dev.blaauwendraad.masker.json;

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;

/**
 * Tracks the current JSONPath segments in the trie.
 */
class JsonPathTracker {
    private static final KeyMatcher.RadixTriePointer NULL_NODE = new KeyMatcher.RadixTriePointer(new KeyMatcher.RadixTrieNode(new byte[0], new byte[0]), 0);

    private final KeyMatcher keyMatcher;
    private final ArrayDeque<KeyMatcher.RadixTriePointer> jsonPathSegments = new ArrayDeque<>();

    JsonPathTracker(KeyMatcher keyMatcher) {
        this.keyMatcher = keyMatcher;
        var root = keyMatcher.getRootNode();
        if (!root.descent((byte) '$')) {
            throw new IllegalStateException("JSONPath root node is null");
        }
        this.jsonPathSegments.push(new KeyMatcher.RadixTriePointer(root));
        root.reset();
    }

    /**
     * Expands the current tracked JSONPath with an array segment.
     */
    void pushArraySegment() {
        jsonPathSegments.push(getWildcardNodeOrNullNode());
    }

    /**
     * Expands the current tracked JSONPath with a value segment.
     */
    void pushKeyValueSegment(byte[] bytes, int keyOffset, int keyLength) {
        jsonPathSegments.push(getKeyValueNodeOrNullNode(bytes, keyOffset, keyLength));
    }

    /**
     * Backtracks the current tracked JSONPath to the previous segment.
     */
    void backtrack() {
        jsonPathSegments.pop();
    }

    /**
     * Traverse the trie node when entering an array. In order to match the array it has to be a wildcard.
     * <p>For example:
     * For a JSON like this {@code { "holder": [ { "maskMe": "secret" } } } the matching JSONPath has to be
     * {@code '$.holder.*.maskMe'}, so that entering the array requires a wildcard node.
     */
    private KeyMatcher.RadixTriePointer getWildcardNodeOrNullNode() {
        var current = currentNode();
        if (current == null) {
            return NULL_NODE;
        }
        try {
            if (!current.descent((byte) '.')) {
                return NULL_NODE;
            }
            if (current.isJsonPathWildcard()) {
                current.descent((byte) '*');
                return new KeyMatcher.RadixTriePointer(current);
            }
            return NULL_NODE;
        } finally {
            current.reset();
        }
    }

    /**
     * Traverse the trie node when entering a key-value. The matching can be done for the matching key, or through a wildcard ('*') JSONPath.
     */
    private KeyMatcher.RadixTriePointer getKeyValueNodeOrNullNode(byte[] bytes, int keyOffset, int keyLength) {
        var current = currentNode();
        if (current == null) {
            return NULL_NODE;
        }
        try {
            if (!current.descent((byte) '.')) {
                return NULL_NODE;
            }
            if (current.isJsonPathWildcard()) {
                current.descent((byte) '*');
                return new KeyMatcher.RadixTriePointer(current);
            } else {
                var child = keyMatcher.traverseFrom(current, bytes, keyOffset, keyLength);
                if (child != null) {
                    return new KeyMatcher.RadixTriePointer(child);
                }
            }
            return NULL_NODE;
        } finally {
            current.reset();
        }
    }

    KeyMatcher.@Nullable RadixTriePointer currentNode() {
        var peek = jsonPathSegments.peek();
        if (peek == NULL_NODE) {
            return null;
        }
        return peek;
    }
}
