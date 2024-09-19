package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;
import dev.blaauwendraad.masker.json.util.Utf8Util;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private final StatefulRadixTrieNode statefulRoot;

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
        this.root = compress(preInitRootNode);
        this.statefulRoot = new StatefulRadixTrieNode(root);
    }

    /**
     * Compresses a pre-initialization trie node into a permanent {@link KeyMatcher} radix trie
     * node. Compression occurs only when a node has multiple children sharing a longest common
     * prefix. In such cases, the common prefix is merged into a continuous prefix to reduce memory
     * usage and optimize the lookups.
     *
     * @param node the node to be compressed
     * @return the compressed pre-initialization trie into a post-initialization trie
     */
    static TrieNode compress(PreInitTrieNode node) {
        List<byte[]> commonPrefix = new ArrayList<>();
        while (true) {
            if (node.endOfWord || node.children.size() != 1) {
                return convertToRadixNode(node, commonPrefix);
            }
            var childBytes = new byte[2];
            commonPrefix.add(childBytes);
            childBytes[0] = node.children.firstKey();
            if (!node.childrenUpper.isEmpty()) {
                childBytes[1] = node.childrenUpper.firstKey();
            }

            node = node.children.firstEntry().getValue();
        }
    }

    private static TrieNode convertToRadixNode(PreInitTrieNode node, List<byte[]> commonPrefix) {
        // reached the end of prefix, create a new node
        byte[] prefix = new byte[commonPrefix.size()];
        byte[] prefixUpper = new byte[commonPrefix.size()];
        for (int i = 0; i < commonPrefix.size(); i++) {
            byte[] prefixes = commonPrefix.get(i);
            prefix[i] = prefixes[0];
            prefixUpper[i] = prefixes[1];
        }

        TrieNode radixNode = new TrieNode(prefix, prefixUpper);
        radixNode.endOfWord = node.endOfWord;
        radixNode.negativeMatch = node.negativeMatch;
        radixNode.keyMaskingConfig = node.keyMaskingConfig;
        if (!node.children.isEmpty()) {
            Map<PreInitTrieNode, TrieNode> transformedNodes = new HashMap<>();
            int childrenArrayOffset = node.children.firstKey();
            int childrenArraySize = node.children.lastKey() - childrenArrayOffset + 1;
            int childrenUpperArrayOffset = -1;
            int childrenUpperArraySize = 0;
            if (!node.childrenUpper.isEmpty()) {
                childrenUpperArrayOffset = node.childrenUpper.firstKey();
                childrenUpperArraySize = node.childrenUpper.lastKey() - childrenUpperArrayOffset + 1;
            }
            radixNode.childrenArrayOffset = childrenArrayOffset;
            radixNode.children = new TrieNode[childrenArraySize];
            radixNode.childrenUpperArrayOffset = childrenUpperArrayOffset;
            radixNode.childrenUpper = new TrieNode[childrenUpperArraySize];

            for (Map.Entry<Byte, PreInitTrieNode> e : node.children.entrySet()) {
                byte b = e.getKey();
                var child = e.getValue();
                radixNode.children[b - radixNode.childrenArrayOffset] = transformedNodes.computeIfAbsent(child, KeyMatcher::compress);
            }

            for (Map.Entry<Byte, PreInitTrieNode> e : node.childrenUpper.entrySet()) {
                byte b = e.getKey();
                var child = e.getValue();
                radixNode.childrenUpper[b - radixNode.childrenUpperArrayOffset] = transformedNodes.computeIfAbsent(child, KeyMatcher::compress);
            }
        }
        return radixNode;
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
            byte[] bytes, int keyOffset, int keyLength, @Nullable StatefulRadixTrieNode currentJsonPathNode) {
        try {
            if (currentJsonPathNode != null) {
                currentJsonPathNode.checkpoint();
            }
            StatefulRadixTrieNode node = currentJsonPathNode;
            if (maskingConfig.isInMaskMode()) {
                // check JSONPath first, as it's more specific
                // if found - mask with this config
                // if not found - do not mask
                if (node != null && node.endOfWord()) {
                    return node.keyMaskingConfig();
                } else if (keyLength != SKIP_KEY_LOOKUP) {
                    // also check regular key
                    node = searchNode(statefulRoot, bytes, keyOffset, keyLength);
                    if (node != null && node.endOfWord()) {
                        return node.keyMaskingConfig();
                    }
                }
                return null;
            } else {
                // check JSONPath first, as it's more specific
                // if found and is not negativeMatch - do not mask
                // if found and is negative match - mask, but with a specific config
                // if not found - mask with default config
                if (node != null && node.endOfWord()) {
                    if (node.negativeMatch()) {
                        return node.keyMaskingConfig();
                    }
                    return null;
                } else if (keyLength != SKIP_KEY_LOOKUP) {
                    // also check regular key
                    node = searchNode(statefulRoot, bytes, keyOffset, keyLength);
                    if (node != null && node.endOfWord()) {
                        if (node.negativeMatch()) {
                            return node.keyMaskingConfig();
                        }
                        return null;
                    }
                }
                return maskingConfig.getDefaultConfig();
            }
        } finally {
            if (currentJsonPathNode != null) {
                currentJsonPathNode.restore();
            }
            statefulRoot.restore();
        }
    }

    /**
     * Searches the trie node by the key offset in the byte array. The node returned might be a prefix,
     * so {@link TrieNode#endOfWord} needs to be checked additionally for full key matching.
     *
     * @param from from which node to do the search, either root node or existing json path node
     * @param bytes the byte array containing the key to be matched
     * @param offset offset of the key in the bytes array
     * @param length length of the key in the bytes array
     * @return the node if found, {@code null} otherwise.
     */
    @Nullable
    private StatefulRadixTrieNode searchNode(StatefulRadixTrieNode from, byte[] bytes, int offset, int length) {
        var node = from;

        int endIndex = offset + length;
        for (int i = offset; i < endIndex; i++) {
            // every character of the input key can be escaped \\uXXXX, but since the KeyMatcher uses byte
            // representation of non-escaped characters of the key (e.g. 'key' -> [107, 101, 121]) in UTF-16 format,
            // we need to make sure to transform individual escaped characters into bytes before matching them against
            // the trie.
            // Any escaped character (6 bytes from the input) represents 1 to 4 bytes of unescaped key,
            // each of the bytes has to be matched against the trie to return a TrieNode
            if (isEncodedCharacter(bytes, i, endIndex)) {
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
                        && isEncodedCharacter(bytes, i, endIndex)) {
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
                byte b = bytes[i];
                node = node.child(b);
            }

            if (node == null) {
                return null;
            }
        }

        return node;
    }

    /**
     * Returns whether the current index contains encoded character, e.g. '\\u0000'
     */
    private static boolean isEncodedCharacter(byte[] bytes, int fromIndex, int toIndex) {
        // -6 for all bytes of the byte encoded unicode character (\\u + 4 hex bytes)
        // to prevent possible ArrayIndexOutOfBoundsExceptions
        return fromIndex <= toIndex - 6 && bytes[fromIndex] == '\\' && bytes[fromIndex + 1] == 'u';
    }

    @Nullable StatefulRadixTrieNode getJsonPathRootNode() {
        return new StatefulRadixTrieNode(root).child((byte) '$');
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
    @Nullable StatefulRadixTrieNode traverseJsonPathSegment(
            byte[] bytes, @Nullable StatefulRadixTrieNode begin, int keyOffset, int keyLength) {
        if (begin == null) {
            return null;
        }
        try {
            var current = begin.child((byte) '.');
            if (current == null) {
                return null;
            }
            if (current.isJsonPathWildcard()) {
                current.child((byte) '*');
                return new StatefulRadixTrieNode(current);
            }

            current = searchNode(current, bytes, keyOffset, keyLength);
            if (current != null) {
                return new StatefulRadixTrieNode(current);
            }
            return null;
        } finally {
            begin.restore();
        }
    }

    public String printTree() {
        return root.toString();
    }

    /**
     * A node in the trie, represents a single byte of the character (if character is ASCII, then
     * represents a single character). An array is used instead of a Map for instant access without
     * type casts.
     *
     * <p>Unlike a regular trie, a radix trie is compressed by merging all nodes that share a common
     * prefix, reducing both memory usage and access time. As a result, each match becomes a stateful
     * operation that requires not only identifying the next child node but also tracking the sequence
     * (i.e., the number of children already matched). Naturally, after each match, the sequence must
     * be incremented, or if a different node is encountered, it should be reset.
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
        public static final TrieNode[] EMPTY = new TrieNode[0];
        final byte[] prefix;
        final byte[] prefixUpper;
        @Nullable
        TrieNode[] children = EMPTY;
        @Nullable
        TrieNode[] childrenUpper = EMPTY;
        int childrenArrayOffset = -1;
        int childrenUpperArrayOffset = -1;
        /**
         * Masking configuration for the key that ends at this node.
         */
        @Nullable
        KeyMaskingConfig keyMaskingConfig = null;
        /**
         * A marker that the character indicates that the key ends at this node.
         */
        boolean endOfWord = false;
        /**
         * Used to store the configuration, but indicate that json-masker is in ALLOW mode and the key is not allowed.
         */
        boolean negativeMatch = false;

        TrieNode(byte[] prefix, byte[] prefixUpper) {
            this.prefix = prefix;
            this.prefixUpper = prefixUpper;
        }

        /**
         * Retrieves a child node by the byte value. Returns {@code null}, if the trie has no matches.
         */
        @Nullable TrieNode child(byte b, int sequence) {
            if (sequence == prefix.length) {
                int offsetIndex = b - childrenArrayOffset;
                TrieNode child = null;
                if (offsetIndex >= 0 && offsetIndex < children.length) {
                    child = children[offsetIndex];
                }
                int offsetUpperIndex = b - childrenUpperArrayOffset;
                if (offsetUpperIndex >= 0 && offsetUpperIndex < childrenUpper.length) {
                    child = childrenUpper[offsetUpperIndex];
                }
                return child;
            } else if (prefix[sequence] == b || prefixUpper[sequence] == b) {
                return this;
            } else {
                return null;
            }
        }

        boolean endOfWord(int sequence) {
            return sequence == prefix.length && endOfWord;
        }

        @Override
        public String toString() {
            return toString(0);
        }

        public String toString(int indent) {
            StringBuilder sb = new StringBuilder();
            sb.append(new String(prefix, StandardCharsets.UTF_8));
            int childrenIndent = indent + prefix.length;
            boolean first = true;
            for (int i = 0; i < children.length; i++) {
                TrieNode child = children[i];
                if (child != null) {
                    if (!first) {
                        sb.append("\n");
                        sb.append(" ".repeat(childrenIndent));
                    }
                    String prefix = " -> " + (char) (i + childrenArrayOffset);
                    sb.append(prefix);
                    sb.append(child.toString(childrenIndent + prefix.length()));
                    first = false;
                }
            }
            // only for root
            if (indent == 0) {
                sb.append("\n");
            }
            return sb.toString();
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
         * @see TrieNode#child(byte, int)
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

    /**
     * A helper class for managing (radix) trie node and the sequence. On top of regular methods
     * used for matching, also contains {@link #checkpoint()} and {@link #restore()} to allow
     * repeated matching that would otherwise pollute the sequence.
     */
    static class StatefulRadixTrieNode {
        private TrieNode node;
        private int sequence;

        private TrieNode checkPointNode;
        private int checkPointSequence;

        StatefulRadixTrieNode(TrieNode node) {
            this.node = this.checkPointNode = node;
            this.sequence = this.checkPointSequence = 0;
        }

        StatefulRadixTrieNode(StatefulRadixTrieNode node) {
            this.node = this.checkPointNode = node.node;
            this.sequence = this.checkPointSequence = node.sequence;
        }

        @Nullable
        StatefulRadixTrieNode child(byte b) {
            var child = node.child(b, sequence++);
            if (child == null) {
                return null;
            } else if (child != node) {
                node = child;
                sequence = 0;
            }
            return this;
        }

        boolean isJsonPathWildcard() {
            return node.child((byte) '*', sequence) != null
                   && (node.endOfWord(sequence + 1)
                       || node.child((byte) '.', sequence + 1) != null);
        }

        boolean endOfWord() {
            return node.endOfWord(sequence);
        }

        boolean negativeMatch() {
            return node.negativeMatch;
        }

        @Nullable
        KeyMaskingConfig keyMaskingConfig() {
            return node.keyMaskingConfig;
        }

        void checkpoint() {
            checkPointNode = node;
            checkPointSequence = sequence;
        }

        void restore() {
            node = checkPointNode;
            sequence = checkPointSequence;
        }

        @Override
        public String toString() {
            return "[sequence: %s] %s".formatted(sequence, node);
        }
    }
}
