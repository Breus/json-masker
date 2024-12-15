package dev.blaauwendraad.masker.json;

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;

class JsonPathState {
    private static final KeyMatcher.StatefulRadixTrieNode NULL_NODE = new KeyMatcher.StatefulRadixTrieNode(new KeyMatcher.RadixTrieNode(new byte[0], new byte[0]), 0);

    private final KeyMatcher keyMatcher;
    private final ArrayDeque<KeyMatcher.StatefulRadixTrieNode> jsonPathSegments = new ArrayDeque<>();

    JsonPathState(KeyMatcher keyMatcher) {
        this.keyMatcher = keyMatcher;
        var root = keyMatcher.getRootNode();
        if (!root.performChildLookup((byte) '$')) {
            throw new IllegalStateException("JSONPath root node is null");
        }
        this.jsonPathSegments.push(new KeyMatcher.StatefulRadixTrieNode(root));
        root.reset();
    }

    /**
     * Expands current JSONPath with an array segment.
     */
    void pushArraySegment() {
        jsonPathSegments.push(getWildcardNodeOrNullNode());
    }

    /**
     * Expands current JSONPath with a value segment.
     */
    void pushKeyValueSegment(byte[] bytes, int keyOffset, int keyLength) {
        jsonPathSegments.push(getKeyValueNodeOrNullNode(bytes, keyOffset, keyLength));
    }

    /**
     * Backtracks current JSONPath to the previous segment.
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
    private KeyMatcher.StatefulRadixTrieNode getWildcardNodeOrNullNode() {
        var current = currentNode();
        if (current == null) {
            return NULL_NODE;
        }
        try {
            if (!current.performChildLookup((byte) '.')) {
                return NULL_NODE;
            }
            if (current.isJsonPathWildcard()) {
                current.performChildLookup((byte) '*');
                return new KeyMatcher.StatefulRadixTrieNode(current);
            }
            return NULL_NODE;
        } finally {
            current.reset();
        }
    }

    /**
     * Traverse the trie node when entering a key-value. The matching can be done for the matching key, or through a wildcard ('*') JSONPath.
     */
    private KeyMatcher.StatefulRadixTrieNode getKeyValueNodeOrNullNode(byte[] bytes, int keyOffset, int keyLength) {
        var current = currentNode();
        if (current == null) {
            return NULL_NODE;
        }
        try {
            if (!current.performChildLookup((byte) '.')) {
                return NULL_NODE;
            }
            if (current.isJsonPathWildcard()) {
                current.performChildLookup((byte) '*');
                return new KeyMatcher.StatefulRadixTrieNode(current);
            } else {
                var child = keyMatcher.traverseFrom(current, bytes, keyOffset, keyLength);
                if (child != null) {
                    return new KeyMatcher.StatefulRadixTrieNode(child);
                }
            }
            return NULL_NODE;
        } finally {
            current.reset();
        }
    }

    KeyMatcher.@Nullable StatefulRadixTrieNode currentNode() {
        var peek = jsonPathSegments.peek();
        if (peek == NULL_NODE) {
            return null;
        }
        return peek;
    }
}
