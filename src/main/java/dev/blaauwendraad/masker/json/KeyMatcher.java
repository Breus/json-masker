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
 * This key matcher is build using a byte radix trie structure to optimize the look-ups for JSON keys
 * in the target key set.
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
 * <p>For masking, we only care whether the key matched or not, so we can use a radix trie to
 * optimize the look-ups.
 *
 * <p>Further, at initialization time, a case-insensitive radix trie is created such that any casing
 * transformations on the looked-up keys during search are avoided.
 *
 * <p>We can also make a radix trie that looks at bytes instead of characters, so that we can use
 * the bytes and offsets directly in the incoming JSON for comparison and make sure there are no
 * allocations at all.
 */
final class KeyMatcher {
    private static final int SKIP_KEY_LOOKUP = -1;
    private final JsonMaskingConfig maskingConfig;
    /**
     * Used for look-ups without JSONPaths being used
     */
    private final RadixTrieNode root;
    /**
     * Used for look-ups in combination with JSONPaths
     */
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
     * node. Compression occurs only when a node has multiple children sharing the longest common
     * prefix. In such cases, the common prefix is merged into a continuous prefix to reduce memory
     * usage and optimize the lookups.
     *
     * @param node the pre-initialization trie node to be compressed
     * @return the compressed pre-initialization trie into a post-initialization radix trie node
     */
    static RadixTrieNode compress(PreInitTrieNode node) {
        List<byte[]> commonPrefix = new ArrayList<>();
        while (true) {
            if (node.endOfWord || node.childrenLowercase.size() != 1) {
                // If there is more than one child node, the prefix is no longer common and can be converted to a radix trie node
                // Alternatively, if the end of the node is reached, it can also be converted to a radix trie node
                return convertToRadixTrieNode(node, commonPrefix);
            }
            var childBytes = new byte[2];
            commonPrefix.add(childBytes);
            childBytes[0] = node.childrenLowercase.firstKey();
            if (!node.childrenUppercase.isEmpty()) {
                childBytes[1] = node.childrenUppercase.firstKey();
            }

            node = node.childrenLowercase.firstEntry().getValue();
        }
    }

    /**
     * This method is used to convert the common prefix and the pre-initialization into a compresed radix trie node
     *
     * @param node         the pre-initialization node
     * @param commonPrefix the common prefix in 2-length byte array for lower and upper case, e.g.: {[b,B], [r,R], [e,E]}
     * @return the resulting radix trie node
     */
    private static RadixTrieNode convertToRadixTrieNode(PreInitTrieNode node, List<byte[]> commonPrefix) {
        // reached the end of prefix, create a new node
        byte[] prefixLowercase = new byte[commonPrefix.size()];
        byte[] prefixUppercase = new byte[commonPrefix.size()];
        for (int i = 0; i < commonPrefix.size(); i++) {
            byte[] prefixes = commonPrefix.get(i);
            prefixLowercase[i] = prefixes[0];
            prefixUppercase[i] = prefixes[1];
        }

        RadixTrieNode radixNode = new RadixTrieNode(prefixLowercase, prefixUppercase);
        radixNode.endOfWord = node.endOfWord;
        radixNode.negativeMatch = node.negativeMatch;
        radixNode.keyMaskingConfig = node.keyMaskingConfig;
        if (!node.childrenLowercase.isEmpty()) {
            Map<PreInitTrieNode, RadixTrieNode> transformedNodes = new HashMap<>();
            int childrenLowercaseArrayOffset = node.childrenLowercase.firstKey();
            int childrenLowercaseArraySize = node.childrenLowercase.lastKey() - childrenLowercaseArrayOffset + 1;
            int childrenUppercaseArrayOffset = -1;
            int childrenUppercaseArraySize = 0;
            if (!node.childrenUppercase.isEmpty()) {
                childrenUppercaseArrayOffset = node.childrenUppercase.firstKey();
                childrenUppercaseArraySize = node.childrenUppercase.lastKey() - childrenUppercaseArrayOffset + 1;
            }
            radixNode.childrenLowercaseArrayOffset = childrenLowercaseArrayOffset;
            radixNode.childrenLowercase = new RadixTrieNode[childrenLowercaseArraySize];
            radixNode.childrenUppercaseArrayOffset = childrenUppercaseArrayOffset;
            radixNode.childrenUppercase = new RadixTrieNode[childrenUppercaseArraySize];

            for (Map.Entry<Byte, PreInitTrieNode> e : node.childrenLowercase.entrySet()) {
                byte b = e.getKey();
                PreInitTrieNode childNode = e.getValue();
                radixNode.childrenLowercase[b - radixNode.childrenLowercaseArrayOffset] = transformedNodes.computeIfAbsent(childNode, KeyMatcher::compress);
            }

            for (Map.Entry<Byte, PreInitTrieNode> e : node.childrenUppercase.entrySet()) {
                byte b = e.getKey();
                PreInitTrieNode childNode = e.getValue();
                radixNode.childrenUppercase[b - radixNode.childrenUppercaseArrayOffset] = transformedNodes.computeIfAbsent(childNode, KeyMatcher::compress);
            }
        }
        return radixNode;
    }

    /**
     * Inserts a word into the pre-initialization trie (represented by the root node).
     *
     * @param node          the pre-initialization trie (root) node
     * @param word          the word to insert
     * @param negativeMatch if true, the key is not allowed and the trie is in ALLOW mode. For
     *                      example, config {@code builder.allow("name", "age").mask("ssn",
     *                      KeyMaskingConfig.builder().maskStringsWith("[redacted]")) } would only allow {@code name}
     *                      and {@code age} to be present in the JSON, it would use default configuration to mask any
     *                      other key, but would specifically mask {@code ssn} with a string "[redacted]". To make it
     *                      possible to store just the masking configuration we insert a "negative match" node, that
     *                      would not be treated as a target key, but provide a fast lookup for the configuration
     */
    void insert(PreInitTrieNode node, String word, boolean negativeMatch) {
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
                    node.addLowercase(lowerBytes[i], child);
                    if (lowerBytes[i] != upperBytes[i]) {
                        node.addUppercase(upperBytes[i], child);
                    }
                } else {
                    node.addLowercase(b, child);
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
     * masked
     */
    @Nullable
    KeyMaskingConfig getMaskConfigIfMatched(
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
     * so {@link RadixTrieNode#endOfWord} needs to be checked additionally for full key matching.
     *
     * @param from   from which node to do the search, either root node or existing json path node
     * @param bytes  the byte array containing the key to be matched
     * @param offset offset of the key in the bytes array
     * @param length length of the key in the bytes array
     * @return the node if found, {@code null} otherwise.
     */
    @Nullable
    private StatefulRadixTrieNode searchNode(StatefulRadixTrieNode from, byte[] bytes, int offset, int length) {
        StatefulRadixTrieNode node = from;
        int endIndex = offset + length;
        for (int i = offset; i < endIndex; i++) {
            // every character of the input key can be escaped \\uXXXX, but since the KeyMatcher uses byte
            // representation of non-escaped characters of the key (e.g. 'key' -> [107, 101, 121]) in UTF-16 format,
            // we need to make sure to transform individual escaped characters into bytes before matching them against
            // the trie.
            // Any escaped character (6 bytes from the input) represents 1 to 4 bytes of unescaped key,
            // each of the bytes has to be matched against the trie to return a TrieNode
            if (isUnicodeEncodedCharacter(bytes, i, endIndex)) {
                char unicodeHexBytesAsChar = Utf8Util.unicodeHexToChar(bytes, i + 2);
                i += 6;
                if (unicodeHexBytesAsChar < 0x80) {
                    // < 128 (in decimal) fits in 7 bits which is 1 byte of data in UTF-8
                    node = node.findMatchingTrieNode((byte) unicodeHexBytesAsChar); // check 1st byte
                } else if (unicodeHexBytesAsChar < 0x800) { // 2048 in decimal,
                    // < 2048 (in decimal) fits in 11 bits which is 2 bytes of data in UTF-8
                    node = node.findMatchingTrieNode((byte) (0xc0 | (unicodeHexBytesAsChar >> 6))); // check 1st byte
                    if (node == null) {
                        return null;
                    }
                    node = node.findMatchingTrieNode((byte) (0x80 | (unicodeHexBytesAsChar & 0x3f))); // check 2nd byte
                } else if (!Character.isSurrogate(unicodeHexBytesAsChar)) {
                    // dealing with characters with values between 2048 and 65536 which
                    // equals to 2^16 or 16 bits, which is 3 bytes of data in UTF-8 encoding
                    node = node.findMatchingTrieNode((byte) (0xe0 | (unicodeHexBytesAsChar >> 12))); // check 1st byte
                    if (node == null) {
                        return null;
                    }
                    node = node.findMatchingTrieNode((byte) (0x80 | ((unicodeHexBytesAsChar >> 6) & 0x3f))); // check 2nd byte
                    if (node == null) {
                        return null;
                    }
                    node = node.findMatchingTrieNode((byte) (0x80 | (unicodeHexBytesAsChar & 0x3f))); // check 3rd byte
                } else {
                    // decoding non-BMP characters in UTF-16 using a pair of high and low
                    // surrogates which together form one unicode character.
                    int codePoint = -1;
                    if (Character.isHighSurrogate(unicodeHexBytesAsChar) // first surrogate must be the high surrogate
                            && isUnicodeEncodedCharacter(bytes, i, endIndex)) {
                        char lowSurrogate = Utf8Util.unicodeHexToChar(bytes, i + 2);
                        if (Character.isLowSurrogate(lowSurrogate)) {
                            codePoint = Character.toCodePoint(unicodeHexBytesAsChar, lowSurrogate);
                        }
                    }
                    if (codePoint < 0) {
                        // the key contains invalid surrogate pair and won't be matched
                        return null;
                    } else {
                        node = node.findMatchingTrieNode((byte) (0xf0 | (codePoint >> 18))); // check 1st byte
                        if (node == null) {
                            return null;
                        }
                        node = node.findMatchingTrieNode((byte) (0x80 | ((codePoint >> 12) & 0x3f))); // check 2nd byte
                        if (node == null) {
                            return null;
                        }
                        node = node.findMatchingTrieNode((byte) (0x80 | ((codePoint >> 6) & 0x3f))); // check 3rd byte
                        if (node == null) {
                            return null;
                        }
                        node = node.findMatchingTrieNode((byte) (0x80 | (codePoint & 0x3f))); // check 4th byte
                    }
                    i += 6;
                }
                i--; // to offset loop increment
            } else {
                byte b = bytes[i];
                node = node.findMatchingTrieNode(b);
            }

            if (node == null) {
                return null;
            }
        }

        return node;
    }

    /**
     * Returns whether the byte sequence between the fromIndex and toIndex represents a unicode encoded character,
     * e.g. '\\u0000', so the length must be 6 and starting with '\\u'.
     *
     * @param bytes     the byte sequence representing some character
     * @param fromIndex start index of the character, inclusive
     * @param toIndex   end index of the character, exclusive
     */
    static boolean isUnicodeEncodedCharacter(byte[] bytes, int fromIndex, int toIndex) {
        // -6 for all bytes of the byte encoded unicode character (\\u + 4 hex bytes)
        // to prevent possible ArrayIndexOutOfBoundsExceptions
        return fromIndex <= toIndex - 6 && bytes[fromIndex] == '\\' && bytes[fromIndex + 1] == 'u';
    }

    @Nullable
    StatefulRadixTrieNode getJsonPathRootNode() {
        return new StatefulRadixTrieNode(root).findMatchingTrieNode((byte) '$');
    }

    /**
     * Traverses the trie along the passed JSONPath segment starting from {@code begin} node. The
     * passed segment is represented as a key {@code (keyOffset, keyLength)} reference in {@code
     * bytes} array.
     *
     * @param bytes     the message bytes.
     * @param begin     a TrieNode from which the traversal begins.
     * @param keyOffset the offset in {@code bytes} of the segment.
     * @param keyLength the length of the segment.
     * @return a TrieNode of the last symbol of the segment. {@code null} if the segment is not in
     * the trie.
     */
    @Nullable
    StatefulRadixTrieNode traverseJsonPathSegment(
            byte[] bytes, @Nullable StatefulRadixTrieNode begin, int keyOffset, int keyLength) {
        if (begin == null) {
            return null;
        }
        try {
            var current = begin.findMatchingTrieNode((byte) '.');
            if (current == null) {
                return null;
            }
            if (current.isJsonPathWildcard()) {
                current.findMatchingTrieNode((byte) '*');
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
     * A node in the radix trie. Represents a single "prefix" of bytes (which can be one or multiple bytes) of a target
     * key or multiple target keys (if the prefix is shared). An array is used instead of a Map for instant access
     * without type casts.
     *
     * <p>Unlike a regular trie, a radix trie is compressed by merging all nodes that share a common
     * prefix, reducing both memory usage and access time. As a result, each match becomes a stateful
     * operation that requires not only identifying the next child node but also tracking the index in the prefix
     * ,i.e., the number of bytes of the prefix already matched. Naturally, after each match, the prefix index must
     * be incremented, or if a different node is encountered, it should be reset.
     *
     * <p>The array starts from a non-null child which represents the byte with value {@link
     * RadixTrieNode#childrenLowercaseArrayOffset} and every subsequent byte is offset by that value.
     *
     * <p>To accommodate the case-insensitivity upper-case characters are stored separately in an
     * additional array {@link RadixTrieNode#childrenUppercase} with its own offset. The reason for splitting
     * the array into two, allows for a more compact memory layout compared to using a single array.
     * In the most common case (single child of upper/lower case character), both arrays are of size
     * 1, while storing them in the same array would result in a gap of 32 {@code null}-elements.
     */
    static class RadixTrieNode {
        public static final RadixTrieNode[] EMPTY = new RadixTrieNode[0];
        final byte[] prefixLowercase;
        final byte[] prefixUppercase;
        @Nullable
        RadixTrieNode[] childrenLowercase = EMPTY;
        @Nullable
        RadixTrieNode[] childrenUppercase = EMPTY;
        int childrenLowercaseArrayOffset = -1;
        int childrenUppercaseArrayOffset = -1;
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

        RadixTrieNode(byte[] prefixLowercase, byte[] prefixUppercase) {
            this.prefixLowercase = prefixLowercase;
            this.prefixUppercase = prefixUppercase;
        }

        /**
         * Retrieves a child node by the byte value if it exists. This can be the same node again if the next
         * value of the prefix matches the byte value, hence the prefixIndex (to look at) is also passed as parameter.s
         *
         * @param prefixIndex the current index in the prefix were we should be looking for the value. If the
         *                    prefixIndex is the same length as the current prefix, we will be looking for a child node
         * @param byteValue   the byte value to look for
         * @return a child node that matches the byte value or {@code null} if there are no matching child nodes
         */
        KeyMatcher.@Nullable RadixTrieNode child(byte byteValue, int prefixIndex) {
            if (prefixIndex == prefixLowercase.length) {
                int offsetIndex = byteValue - childrenLowercaseArrayOffset;
                RadixTrieNode child = null;
                if (offsetIndex >= 0 && offsetIndex < childrenLowercase.length) {
                    child = childrenLowercase[offsetIndex];
                }
                int offsetUpperIndex = byteValue - childrenUppercaseArrayOffset;
                if (offsetUpperIndex >= 0 && offsetUpperIndex < childrenUppercase.length) {
                    child = childrenUppercase[offsetUpperIndex];
                }
                return child;
            } else if (prefixLowercase[prefixIndex] == byteValue || prefixUppercase[prefixIndex] == byteValue) {
                return this;
            } else {
                return null;
            }
        }

        boolean endOfWord(int sequence) {
            return sequence == prefixLowercase.length && endOfWord;
        }

        @Override
        public String toString() {
            return toString(0);
        }

        public String toString(int indent) {
            StringBuilder sb = new StringBuilder();
            sb.append(new String(prefixLowercase, StandardCharsets.UTF_8));
            int childrenIndent = indent + prefixLowercase.length;
            boolean first = true;
            for (int i = 0; i < childrenLowercase.length; i++) {
                RadixTrieNode child = childrenLowercase[i];
                if (child != null) {
                    if (!first) {
                        sb.append("\n");
                        sb.append(" ".repeat(childrenIndent));
                    }
                    String prefix = " -> " + (char) (i + childrenLowercaseArrayOffset);
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
     * this node is compressed into a {@link RadixTrieNode} for more efficient memory layout.
     */
    static class PreInitTrieNode {
        /**
         * @see RadixTrieNode#childrenLowercase
         */
        TreeMap<Byte, PreInitTrieNode> childrenLowercase = new TreeMap<>();

        /**
         * @see RadixTrieNode#childrenUppercase
         */
        TreeMap<Byte, PreInitTrieNode> childrenUppercase = new TreeMap<>();

        /**
         * @see RadixTrieNode#keyMaskingConfig
         */
        @Nullable
        KeyMaskingConfig keyMaskingConfig = null;

        /**
         * @see RadixTrieNode#endOfWord
         */
        boolean endOfWord = false;

        /**
         * @see RadixTrieNode#negativeMatch
         */
        boolean negativeMatch = false;

        /**
         * @see RadixTrieNode#child(byte, int)
         */
        @Nullable
        PreInitTrieNode child(byte b) {
            PreInitTrieNode child = childrenLowercase.get(b);
            if (child != null) {
                return child;
            }
            return childrenUppercase.get(b);
        }

        /**
         * Adds a new child to the trie. When case-insensitivity is enabled this must represent the
         * lower-case byte, otherwise is just a byte in original case.
         */
        void addLowercase(Byte b, PreInitTrieNode child) {
            childrenLowercase.put(b, child);
        }

        /**
         * Adds a child using an equivalent upper-case byte of the child already inserted. When
         * case-insensitivity is enabled this must represent the upper-case byte, otherwise must not
         * be called.
         */
        void addUppercase(Byte b, PreInitTrieNode child) {
            childrenUppercase.put(b, child);
        }


    }

    /**
     * A helper class for managing (radix) trie node and the sequence. On top of regular methods
     * used for matching, also contains {@link #checkpoint()} and {@link #restore()} to allow
     * repeated matching that would otherwise pollute the sequence.
     */
    static class StatefulRadixTrieNode {
        private RadixTrieNode node;
        private int prefixIndex;

        private RadixTrieNode checkPointNode;
        private int checkPointPrefixIndex;

        StatefulRadixTrieNode(RadixTrieNode node) {
            this.node = this.checkPointNode = node;
            this.prefixIndex = this.checkPointPrefixIndex = 0;
        }

        StatefulRadixTrieNode(StatefulRadixTrieNode node) {
            this.node = this.checkPointNode = node.node;
            this.prefixIndex = this.checkPointPrefixIndex = node.prefixIndex;
        }

        /**
         * Finds the matching trie node for the provided byte value. If the next byte in the current radix trie node
         * prefix matches the byte value, return the existing radix trie node with increased prefix index. Otherwise,
         * looks if a child node matches the byte value. If neither match, returns {@code null}.
         *
         * @param byteValue the "next" byte value to find the matching radix trie node for
         * @return the current radix trie node or a child of it, if either of them match. Otherwise, returns
         * {@code null}
         */
        @Nullable
        StatefulRadixTrieNode findMatchingTrieNode(byte byteValue) {
            var child = node.child(byteValue, prefixIndex++);
            if (child == null) {
                return null;
            } else if (child != node) {
                node = child;
                prefixIndex = 0;
            }
            return this;
        }

        boolean isJsonPathWildcard() {
            return node.child((byte) '*', prefixIndex) != null
                    && (node.endOfWord(prefixIndex + 1)
                    || node.child((byte) '.', prefixIndex + 1) != null);
        }

        boolean endOfWord() {
            return node.endOfWord(prefixIndex);
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
            checkPointPrefixIndex = prefixIndex;
        }

        void restore() {
            node = checkPointNode;
            prefixIndex = checkPointPrefixIndex;
        }

        @Override
        public String toString() {
            return "[sequence: %s] %s".formatted(prefixIndex, node);
        }
    }
}
