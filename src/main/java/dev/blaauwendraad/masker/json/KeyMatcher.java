package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;
import dev.blaauwendraad.masker.json.util.Utf8Util;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * This key matcher is build using a byte trie structure to optimize the look-ups for JSON keys in
 * the target key set.
 *
 * <p>The main idea is that we need to know whether a JSON key is in the target key set. One could
 * do a contains on the hash set, which would compute a hashcode for the whole key before doing a
 * "fast" lookup. Another option would be to iterate over target keys and compare characters one by
 * one for each key, given that in reality most keys would fail fast (assuming nobody asks us to
 * mask keys {@code 'a[...]b'} in JSONs with keys {@code 'aa[...]b'})
 *
 * <p>Both options are not ideal, because:
 *
 * <ul>
 *   <li>we expect set of target keys to be relatively small (<100 keys)
 *   <li>we expect target keys themselves to be relatively small (<100 characters)
 *   <li>keys are case-insensitive by default, meaning that we have to do toLowerCase for every
 *       incoming key
 * </ul>
 *
 * <p>For masking, we only care whether the key matched or not, so we can use a trie to optimize the
 * look-ups.
 *
 * <p>Further, at initialization time, a case-insensitive trie is created such that any casing
 * transformations on the looked-up keys during search are avoided.
 *
 * <p>We can also make a trie that looks at bytes instead of characters, so that we can use the
 * bytes and offsets directly in the incoming JSON for comparison and make sure there are no
 * allocations at all.
 */
final class KeyMatcher {
    private static final int SKIP_KEY_LOOKUP = -1;
    private final JsonMaskingConfig maskingConfig;
    private final TrieNode root;

    public KeyMatcher(JsonMaskingConfig maskingConfig) {
        this.maskingConfig = maskingConfig;
        PreInitTrieNode preInitRootNode = new PreInitTrieNode();
        maskingConfig.getTargetKeys().forEach(key -> insert(preInitRootNode, key, false));
        maskingConfig.getTargetJsonPaths().forEach(jsonPath -> insert(preInitRootNode, jsonPath.toString(), false));
        if (maskingConfig.isInAllowMode()) {
            // in allow mode we might have a specific configuration for the masking key
            // see ByteTrie#insert documentation for more details
            maskingConfig.getKeyConfigs().keySet().forEach(key -> insert(preInitRootNode, key, true));
        }
        this.root = transform(preInitRootNode);
    }

    /**
     * Transforms a (temporary) pre-initialization node into a permanent {@link KeyMatcher} look-up
     * trie node. This is done by applying transformations of each (child) node starting from the
     * root pre-init node and following a BFS order subsequently.
     *
     * @param preInitNode the node which will be transformed by having all its children transformed
     * @return the transformed pre-initialization trie into a post-initialization trie
     */
    static TrieNode transform(PreInitTrieNode preInitNode) {
        Map<PreInitTrieNode, TrieNode> transformedNodes = new HashMap<>();
        Deque<PreInitTrieNode> stack = new ArrayDeque<>();
        stack.push(preInitNode);
        while (!stack.isEmpty()) {
            PreInitTrieNode currentPreInitNode = stack.pop();
            if (transformedNodes.containsKey(currentPreInitNode)) {
                // lower-case and upper-case children represented by the same exact node under a different index
                // avoid transforming the children that were already transformed
                continue;
            }
            int childrenArrayOffset = -1;
            int childrenArraySize = 0;
            int childrenUpperArrayOffset = -1;
            int childrenUpperArraySize = 0;
            if (!currentPreInitNode.children.isEmpty()) {
                childrenArrayOffset = currentPreInitNode.children.firstKey();
                childrenArraySize = currentPreInitNode.children.lastKey() - childrenArrayOffset + 1;
                if (!currentPreInitNode.childrenUpper.isEmpty()) {
                    childrenUpperArrayOffset = currentPreInitNode.childrenUpper.firstKey();
                    childrenUpperArraySize = currentPreInitNode.childrenUpper.lastKey() - childrenUpperArrayOffset + 1;
                }
            }
            TrieNode currentNode =
                    new TrieNode(
                            childrenArrayOffset,
                            childrenUpperArrayOffset,
                            childrenArraySize == 0 ? TrieNode.EMPTY_CHILDREN : new TrieNode[childrenArraySize],
                            childrenUpperArraySize == 0
                                    ? TrieNode.EMPTY_CHILDREN
                                    : new TrieNode[childrenUpperArraySize],
                            currentPreInitNode.keyMaskingConfig,
                            currentPreInitNode.endOfWord,
                            currentPreInitNode.negativeMatch);
            transformedNodes.put(currentPreInitNode, currentNode);
            stack.addAll(currentPreInitNode.children.values());
            stack.addAll(currentPreInitNode.childrenUpper.values());
        }

        for (Map.Entry<PreInitTrieNode, TrieNode> entry : transformedNodes.entrySet()) {
            PreInitTrieNode currentPreInitNode = entry.getKey();
            TrieNode currentNode = entry.getValue();

            currentPreInitNode.children.forEach(
                    (byteValue, childNode) ->
                            currentNode.children[byteValue - currentNode.childrenArrayOffset] =
                                    transformedNodes.get(childNode));
            currentPreInitNode.childrenUpper.forEach(
                    (byteValue, childNode) ->
                            currentNode.childrenUpper[byteValue - currentNode.childrenUpperArrayOffset] =
                                    transformedNodes.get(childNode));
        }

        return Objects.requireNonNull(transformedNodes.get(preInitNode));
    }

    /**
     * Inserts a word into the pre-initialization trie (represented by the root node).
     *
     * @param node the pre-initialization trie (root) node
     * @param word the word to insert
     * @param negativeMatch if true, the key is not allowed and the trie is in ALLOW mode. For
     *     example, config {@code builder.allow("name", "age").mask("ssn",
     *     KeyMaskingConfig.builder().maskStringsWith("[redacted]")) } would only allow {@code name}
     *     and {@code age} to be present in the JSON, it would use default configuration to mask any
     *     other key, but would specifically mask {@code ssn} with a string "[redacted]". To make it
     *     possible to store just the masking configuration we insert a "negative match" node, that
     *     would not be treated as a target key, but provide a fast lookup for the configuration
     */
    private void insert(PreInitTrieNode node, String word, boolean negativeMatch) {
        boolean caseInsensitive = !maskingConfig.caseSensitiveTargetKeys();
        byte[] bytes = word.getBytes(StandardCharsets.UTF_8);
        byte[] lowerBytes = null;
        byte[] upperBytes = null;
        if (caseInsensitive) {
            lowerBytes = word.toLowerCase().getBytes(StandardCharsets.UTF_8);
            upperBytes = word.toUpperCase().getBytes(StandardCharsets.UTF_8);
            /*
             from inspecting the code, it looks like lower casing a character does not change the byte length
             on the same encoding, however the documentation explicitly mentions that resulting length might be
             different so better to fail fast if instead of ignoring that. Given that we're doing that only for
             target keys, the idea that it's going to have different lengths is quite unlikely.
            */
            if (bytes.length != lowerBytes.length || bytes.length != upperBytes.length) {
                throw new IllegalArgumentException("Case insensitive trie does not support all characters in " + word);
            }
        }
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            PreInitTrieNode child = node.child(b);
            if (child == null) {
                child = new PreInitTrieNode();
                if (caseInsensitive) {
                    Objects.requireNonNull(lowerBytes);
                    Objects.requireNonNull(upperBytes);
                    /*
                     when case-insensitive we need to keep track of siblings to be able to find the correct node
                     so that we have this structure:
                     <p>
                     (h | H) -> (e | E) -> (l | L) -> (l | L) -> (o | O)
                     <p>
                     and we can travel the tree forward and kinda sideways

                     Also using both toLowerCase and toUpperCase due to
                       1. Locale issues (see String#equalsIgnoreCase)
                       2. So we don't have to convert when searching
                    */
                    node.add(lowerBytes[i], child);
                    if (lowerBytes[i] != upperBytes[i]) {
                        node.addUpper(upperBytes[i], child);
                    }
                } else {
                    node.add(b, child);
                }
            }
            node = child;
        }
        node.keyMaskingConfig = maskingConfig.getConfig(word);
        node.endOfWord = true;
        node.negativeMatch = negativeMatch;
    }

    /**
     * Returns a masking configuration if the key must be masked. Handles both allow and mask mode:
     *
     * <ul>
     *   <li>in allow mode: if the key was explicitly allowed returns null, otherwise returns a
     *       config to mask the key with.
     *   <li>in mask mode: if the key was explicitly masked returns a config to mask the key with,
     *       otherwise returns null.
     * </ul>
     *
     * <p>When key is to be masked (return value != null) and the key had specific masking config
     * returns that, if not - returns default masking config.
     *
     * <p>When key is to be masked (return value != null) and the key had specific masking config
     * returns that, if not - returns default masking config.
     *
     * @return the config if the key needs to be masked, {@code null} if key does not need to be
     *     masked
     */
    @Nullable KeyMaskingConfig getMaskConfigIfMatched(
            byte[] bytes, int keyOffset, int keyLength, @Nullable TrieNode currentJsonPathNode) {
        // first search by key
        TrieNode node = currentJsonPathNode;
        if (maskingConfig.isInMaskMode()) {
            // check JSONPath first, as it's more specific
            // if found - mask with this config
            // if not found - do not mask
            if (node != null && node.endOfWord && !node.negativeMatch) {
                return node.keyMaskingConfig;
            } else if (keyLength != SKIP_KEY_LOOKUP) {
                // also check regular key
                node = searchNode(bytes, keyOffset, keyLength);
                if (node != null && !node.negativeMatch) {
                    return node.keyMaskingConfig;
                }
            }
            return null;
        } else {
            // check JSONPath first, as it's more specific
            // if found and is not negativeMatch - do not mask
            // if found and is negative match - mask, but with a specific config
            // if not found - mask with default config
            if (node != null && node.endOfWord) {
                if (node.negativeMatch) {
                    return node.keyMaskingConfig;
                }
                return null;
            } else if (keyLength != SKIP_KEY_LOOKUP) {
                // also check regular key
                node = searchNode(bytes, keyOffset, keyLength);
                if (node != null) {
                    if (node.negativeMatch) {
                        return node.keyMaskingConfig;
                    }
                    return null;
                }
            }
            return maskingConfig.getDefaultConfig();
        }
    }

    @Nullable
    private TrieNode searchNode(byte[] bytes, int offset, int length) {
        TrieNode node = root;

        for (int i = offset; i < offset + length; i++) {
            byte b = bytes[i];
            // every character of the input key can be escaped \\uXXXX, but since the KeyMatcher uses byte
            // representation of non-escaped characters of the key (e.g. 'key' -> [107, 101, 121]) in UTF-16 format,
            // we need to make sure to transform individual escaped characters into bytes before matching them against
            // the trie.
            // Any escaped character (6 bytes from the input) represents 1 to 4 bytes of unescaped key,
            // each of the bytes has to be matched against the trie to return a TrieNode
            if (b == '\\' && bytes[i + 1] == 'u' && i <= offset + length - 6) {
                char unicodeHexBytesAsChar = Utf8Util.unicodeHexToChar(bytes, i + 2);
                i += 6;
                if (unicodeHexBytesAsChar < 0x80) {
                    // < 128 (in decimal) fits in 7 bits which is 1 byte of data in UTF-8
                    node = node.child((byte) unicodeHexBytesAsChar); // check 1st byte
                } else if (unicodeHexBytesAsChar < 0x800) { // 2048 in decimal,
                    // < 2048 (in decimal) fits in 11 bits which is 2 bytes of data in UTF-8
                    node = node.child((byte) (0xc0 | (unicodeHexBytesAsChar >> 6))); // check 1st byte
                    if (node == null) {
                        return null;
                    }
                    node = node.child((byte) (0x80 | (unicodeHexBytesAsChar & 0x3f))); // check 2nd byte
                } else if (!Character.isSurrogate(unicodeHexBytesAsChar)) {
                    // dealing with characters with values between 2048 and 65536 which
                    // equals to 2^16 or 16 bits, which is 3 bytes of data in UTF-8 encoding
                    node = node.child((byte) (0xe0 | (unicodeHexBytesAsChar >> 12))); // check 1st byte
                    if (node == null) {
                        return null;
                    }
                    node = node.child((byte) (0x80 | ((unicodeHexBytesAsChar >> 6) & 0x3f))); // check 2nd byte
                    if (node == null) {
                        return null;
                    }
                    node = node.child((byte) (0x80 | (unicodeHexBytesAsChar & 0x3f))); // check 3rd byte
                } else {
                    // decoding non-BMP characters in UTF-16 using a pair of high and low
                    // surrogates which together form one unicode character.
                    int codePoint = -1;
                    if (Character.isHighSurrogate(unicodeHexBytesAsChar) // first surrogate must be the high surrogate
                            && i
                                    <= offset
                                            + length
                                            - 6 /* -6 for all bytes of the byte encoded unicode character (\\u + 4 hex bytes) to prevent possible ArrayIndexOutOfBoundsExceptions */
                            && bytes[i] == '\\' // the high surrogate must be followed by a low surrogate (starting with
                            // \\u)
                            && bytes[i + 1] == 'u') {
                        char lowSurrogate = Utf8Util.unicodeHexToChar(bytes, i + 2);
                        if (Character.isLowSurrogate(lowSurrogate)) {
                            codePoint = Character.toCodePoint(unicodeHexBytesAsChar, lowSurrogate);
                        }
                    }
                    if (codePoint < 0) {
                        // the key contains invalid surrogate pair and won't be matched
                        return null;
                    } else {
                        node = node.child((byte) (0xf0 | (codePoint >> 18))); // check 1st byte
                        if (node == null) {
                            return null;
                        }
                        node = node.child((byte) (0x80 | ((codePoint >> 12) & 0x3f))); // check 2nd byte
                        if (node == null) {
                            return null;
                        }
                        node = node.child((byte) (0x80 | ((codePoint >> 6) & 0x3f))); // check 3rd byte
                        if (node == null) {
                            return null;
                        }
                        node = node.child((byte) (0x80 | (codePoint & 0x3f))); // check 4th byte
                    }
                    i += 6;
                }
                i--; // to offset loop increment
            } else {
                node = node.child(b);
            }

            if (node == null) {
                return null;
            }
        }

        if (!node.endOfWord) {
            return null;
        }

        return node;
    }

    @Nullable TrieNode getJsonPathRootNode() {
        return root.child((byte) '$');
    }

    /**
     * Traverses the trie along the passed JSONPath segment starting from {@code begin} node. The
     * passed segment is represented as a key {@code (keyOffset, keyLength)} reference in {@code
     * bytes} array.
     *
     * @param bytes the message bytes.
     * @param begin a TrieNode from which the traversal begins.
     * @param keyOffset the offset in {@code bytes} of the segment.
     * @param keyLength the length of the segment.
     * @return a TrieNode of the last symbol of the segment. {@code null} if the segment is not in
     *     the trie.
     */
    @Nullable TrieNode traverseJsonPathSegment(
            byte[] bytes, @Nullable TrieNode begin, int keyOffset, int keyLength) {
        if (begin == null) {
            return null;
        }
        TrieNode current = begin.child((byte) '.');
        if (current == null) {
            return null;
        }
        TrieNode wildcardLookAhead = current.child((byte) '*');
        if (wildcardLookAhead != null && (wildcardLookAhead.endOfWord || wildcardLookAhead.child((byte) '.') != null)) {
            return wildcardLookAhead;
        }
        for (int i = keyOffset; i < keyOffset + keyLength; i++) {
            byte b = bytes[i];
            current = current.child(b);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /**
     * A node in the trie, represents a single byte of the character (if character is ASCII, then
     * represents a single character). An array is used instead of a Map for instant access without
     * type casts.
     *
     * <p>The array starts from a non-null child which represents the byte with value {@link
     * TrieNode#childrenArrayOffset} and every subsequent byte is offset by that value.
     *
     * <p>To accommodate the case-insensitivity upper-case characters are stored separately in an
     * additional array {@link TrieNode#childrenUpper} with its own offset. The reason for splitting
     * the array into two, allows for a more compact memory layout compared to using a single array.
     * In the most common case (single child of upper/lower case character), both arrays are of size
     * 1, while storing them in the same array would result in a gap of 32 {@code null}-elements.
     */
    static class TrieNode {
        private static final TrieNode[] EMPTY_CHILDREN = new TrieNode[0];

        /**
         * Indicates the indexing offset of the children array. So let's say this value is 65 (ASCII
         * 'A'), then 0th index represents this byte and the 20th index in the array would represent
         * the byte value 85 (ASCII 'U'). This is essentially a memory optimization to not store 256
         * references for the children, but much less in most practical cases at the cost of storing
         * the offset itself (4 bytes).
         */
        private final int childrenArrayOffset;

        private final int childrenUpperArrayOffset;

        @Nullable TrieNode[] children;
        @Nullable TrieNode[] childrenUpper;

        /** Masking configuration for the key that ends at this node. */
        private final @Nullable KeyMaskingConfig keyMaskingConfig;

        /** A marker that the character indicates that the key ends at this node. */
        private final boolean endOfWord;

        /**
         * Used to store the configuration, but indicate that json-masker is in ALLOW mode and the
         * key is not allowed.
         */
        private final boolean negativeMatch;

        TrieNode(
                int childrenArrayOffset,
                int childrenUpperArrayOffset,
                TrieNode[] children,
                TrieNode[] childrenUpper,
                @Nullable KeyMaskingConfig keyMaskingConfig,
                boolean endOfWord,
                boolean negativeMatch) {
            this.childrenArrayOffset = childrenArrayOffset;
            this.childrenUpperArrayOffset = childrenUpperArrayOffset;
            this.children = children;
            this.childrenUpper = childrenUpper;
            this.keyMaskingConfig = keyMaskingConfig;
            this.endOfWord = endOfWord;
            this.negativeMatch = negativeMatch;
        }

        /**
         * Retrieves a child node by the byte value. Returns {@code null}, if the trie has no
         * matches.
         */
        @Nullable TrieNode child(byte b) {
            int offsetIndex = b - childrenArrayOffset;
            // This Sonar/IntelliJ warning on the next line is incorrect because the NullAway bug
            if (offsetIndex >= 0 && offsetIndex < children.length && children[offsetIndex] != null) {
                return children[offsetIndex];
            }
            int offsetUpperIndex = b - childrenUpperArrayOffset;
            if (offsetUpperIndex >= 0 && offsetUpperIndex < childrenUpper.length) {
                return childrenUpper[offsetUpperIndex];
            }
            return null;
        }
    }

    /**
     * This TrieNode represents a temporary trie that is being built. After all keys are inserted,
     * this node is compressed into a {@link TrieNode} for more efficient memory layout.
     */
    static class PreInitTrieNode {
        /**
         * @see TrieNode#children
         */
        TreeMap<Byte, PreInitTrieNode> children = new TreeMap<>();

        /**
         * @see TrieNode#childrenUpper
         */
        TreeMap<Byte, PreInitTrieNode> childrenUpper = new TreeMap<>();

        /**
         * @see TrieNode#keyMaskingConfig
         */
        @Nullable KeyMaskingConfig keyMaskingConfig = null;

        /**
         * @see TrieNode#endOfWord
         */
        boolean endOfWord = false;

        /**
         * @see TrieNode#negativeMatch
         */
        boolean negativeMatch = false;

        /**
         * @see TrieNode#child(byte)
         */
        @Nullable PreInitTrieNode child(byte b) {
            PreInitTrieNode child = children.get(b);
            if (child != null) {
                return child;
            }
            return childrenUpper.get(b);
        }

        /**
         * Adds a new child to the trie. When case-insensitivity is enabled this must represent the
         * lower-case byte, otherwise is just a byte in original case.
         */
        void add(Byte b, PreInitTrieNode child) {
            children.put(b, child);
        }

        /**
         * Adds a child using an equivalent upper-case byte of the child already inserted. When
         * case-insensitivity is enabled this must represent the upper-case byte, otherwise must not
         * be called.
         */
        void addUpper(Byte b, PreInitTrieNode child) {
            childrenUpper.put(b, child);
        }
    }
}
