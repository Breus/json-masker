package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;
import dev.blaauwendraad.masker.json.util.Utf8Util;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;

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
 * <p>Both options are not ideal because we:
 *
 * <ul>
 *   <li>we expect set of target keys to be relatively small (<100 keys)
 *   <li>we expect target keys themselves to also be relatively small (<100 characters)
 *   <li>keys are case-insensitive by default, meaning that we have to do toLowerCase for every
 *       incoming key
 * </ul>
 *
 * <p>For masking, we only care whether the key matched or not, so we can use a Trie to optimize the
 * look-ups.
 *
 * <p>Further, at initialization time we can construct a Trie that is case-insensitive, so that for
 * we can avoid any transformations on the incoming keys during the search.
 *
 * <p>We can also make a Trie that looks at bytes instead of characters, so that we can use the
 * bytes and offsets directly in the incoming JSON for comparison and make sure there are no
 * allocations at all.
 *
 * <p>And lastly, we can make a small optimization to remember all the distinct lengths of the
 * target keys, so that we can fail fast if the incoming key is not of the same length.
 */
final class KeyMatcher {
    private static final int BYTE_OFFSET = -1 * Byte.MIN_VALUE;
    private static final int SKIP_KEY_LOOKUP = -1;
    private final JsonMaskingConfig maskingConfig;
    private final TrieNode root;

    public KeyMatcher(JsonMaskingConfig maskingConfig) {
        this.maskingConfig = maskingConfig;
        PreInitTrieNode preInit = new PreInitTrieNode();
        maskingConfig.getTargetKeys().forEach(key -> insert(preInit, key, false));
        maskingConfig.getTargetJsonPaths().forEach(jsonPath -> insert(preInit, jsonPath.toString(), false));
        if (maskingConfig.isInAllowMode()) {
            // in allow mode we might have a specific configuration for the masking key
            // see ByteTrie#insert documentation for more details
            maskingConfig.getKeyConfigs().keySet().forEach(key -> insert(preInit, key, true));
        }
        this.root = transform(preInit);
    }

    private static TrieNode transform(PreInitTrieNode preInitNode) {
        // Using a stack to simulate the recursion
        Map<PreInitTrieNode, TrieNode> transformedNodes = new HashMap<>();
        Deque<PreInitTrieNode> stack = new ArrayDeque<>();
        stack.push(preInitNode);

        while (!stack.isEmpty()) {
            PreInitTrieNode currentPreInitNode = stack.pop();
            if (transformedNodes.containsKey(currentPreInitNode)) {
                continue;
            }
            TrieNode currentNode = new TrieNode();
            currentNode.keyMaskingConfig = currentPreInitNode.keyMaskingConfig;
            currentNode.endOfWord = currentPreInitNode.endOfWord;
            currentNode.negativeMatch = currentPreInitNode.negativeMatch;

            if (!currentPreInitNode.children.isEmpty()) {
                currentNode.childrenArrayOffset = currentPreInitNode.children.firstKey();
                currentNode.children = new TrieNode[currentPreInitNode.children.lastKey() - currentNode.childrenArrayOffset + 1];

                if (!currentPreInitNode.childrenUpper.isEmpty()) {
                    currentNode.childrenUpperArrayOffset = currentPreInitNode.childrenUpper.firstKey();
                    currentNode.childrenUpper = new TrieNode[currentPreInitNode.childrenUpper.lastKey() - currentNode.childrenUpperArrayOffset + 1];
                }
            }
            transformedNodes.put(currentPreInitNode, currentNode);
            stack.addAll((currentPreInitNode.children.values()));
            stack.addAll((currentPreInitNode.childrenUpper.values()));
        }

        for (Map.Entry<PreInitTrieNode, TrieNode> entry : transformedNodes.entrySet()) {
            PreInitTrieNode currentPreInitNode = entry.getKey();
            TrieNode currentNode = entry.getValue();

            currentPreInitNode.children.forEach(
                    (b, child) -> {
                        currentNode.children[b - currentNode.childrenArrayOffset] = transformedNodes.get(child);
                    });
            currentPreInitNode.childrenUpper.forEach(
                    (b, child) -> {
                        currentNode.childrenUpper[b - currentNode.childrenUpperArrayOffset] = transformedNodes.get(child);
                    });
        }

        return Objects.requireNonNull(transformedNodes.get(preInitNode));
    }

    /**
     * Inserts a word into the pre-initialization trie.
     *
     * @param node the pre-initizalization trie node
     * @param word the word to insert.
     * @param negativeMatch if true, the key is not allowed and the trie is in ALLOW mode.
     *                      For example, config {@code builder.allow("name", "age").mask("ssn",
     *                      KeyMaskingConfig.builder().maskStringsWith("[redacted]")) } would only allow {@code name}
     *                      and {@code age} to be present in the JSON, it would use default configuration to mask any
     *                      other key, but would specifically mask {@code ssn} with a string "[redacted]". To make it
     *                      possible to store just the masking configuration we insert a "negative match" node, that
     *                      would not be treated as a target key, but provide a fast lookup for the configuration
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
             different
             so better to fail fast if instead of ignoring that.

             Given that we're doing that only for target keys, the idea that it's going to have different lengths
             is quite unlikely
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
    @Nullable
    KeyMaskingConfig getMaskConfigIfMatched(byte[] bytes, int keyOffset, int keyLength, @Nullable TrieNode currentJsonPathNode) {
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
                        && i <= offset + length - 6 /* -6 for all bytes of the byte encoded unicode character (\\u + 4 hex bytes) to prevent possible ArrayIndexOutOfBoundsExceptions */
                        && bytes[i] == '\\' // the high surrogate must be followed by a low surrogate (starting with \\u)
                        && bytes[i + 1] == 'u'
                    ) {
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

    @Nullable
    TrieNode getJsonPathRootNode() {
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
            byte[] bytes, @Nullable final TrieNode begin, int keyOffset, int keyLength) {
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
     * A node in the Trie, represents part of the character (if character is ASCII, then represents
     * a single character). A padding of 128 is used to store references to the next positive and
     * negative bytes (which range from -128 to 128, hence the padding).
     */
    static class TrieNode {
        public static final TrieNode[] EMPTY_CHILDREN = new TrieNode[0];
        /**
         * Indicates the indexing offset of the children array. So let's say this value is 10, then
         * the 20th index in the array actually represent the byte value '30'. This is essentially a
         * memory optimization to not store 256 references for the children, but much less in most
         * practical cases.
         */
        int childrenArrayOffset;
        int childrenUpperArrayOffset;

        TrieNode[] children = EMPTY_CHILDREN;
        TrieNode[] childrenUpper = EMPTY_CHILDREN;

        /** A marker that the character indicates that the key ends at this node. */
        boolean endOfWord = false;

        /** Masking configuration for the key that ends at this node. */
        @Nullable KeyMaskingConfig keyMaskingConfig = null;

        /**
         * Used to store the configuration, but indicate that json-masker is in ALLOW mode and the
         * key is not allowed.
         */
        boolean negativeMatch = false;

        /**
         * Retrieves a child node by the byte value. Returns {@code null}, if the trie has no
         * matches.
         */
        @Nullable TrieNode child(byte b) {
            int offsetIndex = b - childrenArrayOffset;
            TrieNode child = null;
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

    static class PreInitTrieNode {
        TreeMap<Integer, PreInitTrieNode> children = new TreeMap<>();
        TreeMap<Integer, PreInitTrieNode> childrenUpper = new TreeMap<>();

        /** A marker that the character indicates that the key ends at this node. */
        boolean endOfWord = false;

        /** Masking configuration for the key that ends at this node. */
        @Nullable KeyMaskingConfig keyMaskingConfig = null;

        /**
         * Used to store the configuration, but indicate that json-masker is in ALLOW mode and the
         * key is not allowed.
         */
        boolean negativeMatch = false;

        /**
         * Retrieves a child node by the byte value. Returns {@code null}, if the trie has no
         * matches.
         */
        @Nullable PreInitTrieNode child(byte b) {
            PreInitTrieNode child = children.get((int) b);
            if (child != null) {
                return child;
            }
            return childrenUpper.get((int) b);
        }

        /** Adds a new child to the trie. */
        void add(Byte b, PreInitTrieNode child) {
            children.put((int) b, child);
        }

        /** Adds a new child to the trie. */
        void addUpper(Byte b, PreInitTrieNode child) {
            childrenUpper.put((int) b, child);
        }
    }
}
