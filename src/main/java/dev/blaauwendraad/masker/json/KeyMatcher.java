package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;
import dev.blaauwendraad.masker.json.util.Utf8Util;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * This key matcher is build using a byte trie structure to optimize the look-ups for JSON keys in the target key set.
 * <p>
 * The main idea is that we need to know whether a JSON key is in the target key set. One could do a contains on
 * the hash set, which would compute a hashcode for the whole key before doing a "fast" lookup. Another option would be
 * to iterate over target keys and compare characters one by one for each key, given that in reality most keys would
 * fail fast (assuming nobody asks us to mask keys {@code 'a[...]b'} in JSONs with keys {@code 'aa[...]b'})
 *
 * <p>
 * Both options are not ideal because
 * we:
 * <ul>
 *   <li>we expect set of target keys to be relatively small (<100 keys)</li>
 *   <li>we expect target keys themselves to also be relatively small (<100 characters)</li>
 *   <li>keys are case-insensitive by default, meaning that we have to do toLowerCase for every incoming key</li>
 * </ul>
 *
 * <p> For masking, we only care whether the key matched or not, so we can use a Trie to optimize the look-ups.
 * <p> Further, at initialization time we can construct a Trie that is case-insensitive, so that for we can avoid any
 * transformations on the incoming keys during the search.
 * <p> We can also make a Trie that looks at bytes instead of characters, so that we can use the bytes and offsets
 * directly in the incoming JSON for comparison and make sure there are no allocations at all.
 * <p> And lastly, we can make a small optimization to remember all the distinct lengths of the target keys, so that
 * we can fail fast if the incoming key is not of the same length.
 */
final class KeyMatcher {
    private static final int BYTE_OFFSET = -1 * Byte.MIN_VALUE;
    private static final int SKIP_KEY_LOOKUP = -1;
    private final JsonMaskingConfig maskingConfig;
    private final TrieNode root;

    public KeyMatcher(JsonMaskingConfig maskingConfig) {
        this.maskingConfig = maskingConfig;
        this.root = new TrieNode();
        maskingConfig.getTargetKeys().forEach(key -> insert(key, false));
        maskingConfig.getTargetJsonPaths().forEach(jsonPath -> insert(jsonPath.toString(), false));
        if (maskingConfig.isInAllowMode()) {
            // in allow mode we might have a specific configuration for the masking key
            // see ByteTrie#insert documentation for more details
            maskingConfig.getKeyConfigs().keySet().forEach(key -> insert(key, true));
        }
    }

    /**
     * Inserts a word into the trie.
     *
     * @param word          the word to insert.
     * @param negativeMatch if true, the key is not allowed and the trie is in ALLOW mode.
     *                      for example config
     *                      {@code
     *                      builder.allow("name", "age").mask("ssn", KeyMaskingConfig.builder().maskStringsWith("[redacted]"))
     *                      }
     *                      would only allow {@code name} and {@code age} to be present in the JSON, it would use
     *                      default configuration to mask any other key, but would specifically mask {@code ssn} with
     *                      a string "[redacted]". To make it possible to store just the masking configuration we
     *                      insert a "negative match" node, that would not be treated as a target key, but provide
     *                      a fast lookup for the configuration
     */
    private void insert(String word, boolean negativeMatch) {
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
        TrieNode node = root;
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            TrieNode child = node.child(b);
            if (child == null) {
                child = new TrieNode();
                node.add(b, child);
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
                    node.add(upperBytes[i], child);
                }
            }
            node = child;
        }
        node.keyMaskingConfig = maskingConfig.getConfig(word);
        node.endOfWord = true;
        node.negativeMatch = negativeMatch;
    }

    /**
     * Returns a masking configuration if the key must be masked.
     * Handles both allow and mask mode:
     * - in allow mode: if the key was explicitly allowed returns null, otherwise returns a config to mask the key with.
     * - in mask mode: if the key was explicitly masked returns a config to mask the key with, otherwise returns null.
     * <p>
     * When key is to be masked (return value != null) and the key had specific masking config returns that, if not -
     * returns default masking config.
     *
     * @return the config if the key needs to be masked, {@code null} if key does not need to be masked
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
            if (b == '\\' && bytes[i + 1] == 'u' && i <= offset + length - 6) {
                int valueStartIndex = i;
                char unicodeHexBytesAsChar = Utf8Util.unicodeHexToChar(bytes, i + 2);
                i += 5; // -1 to offset loop increment
                if (unicodeHexBytesAsChar < 0x80) {
                    // < 128 (in decimal) fits in 7 bits which is 1 byte of data in UTF-8
                    node = node.child((byte) unicodeHexBytesAsChar);
                } else if (unicodeHexBytesAsChar < 0x800) { // 2048 in decimal,
                    // < 2048 (in decimal) fits in 11 bits which is 2 bytes of data in UTF-8
                    node = node.child((byte) (0xc0 | (unicodeHexBytesAsChar >> 6)));
                    if (node == null) {
                        return null;
                    }
                    node = node.child((byte) (0x80 | (unicodeHexBytesAsChar & 0x3f)));
                } else if (Character.isSurrogate(unicodeHexBytesAsChar)) {
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
                        // default String behaviour is to replace invalid surrogate pairs
                        // with the character '?', but from the JSON perspective,
                        // it's better to throw an InvalidJsonException
                        String invalidValue = new String(bytes, valueStartIndex, i - valueStartIndex, StandardCharsets.UTF_8);
                        throw new InvalidJsonException("Invalid surrogate pair '%s' at index %s".formatted(invalidValue, offset + i));
                    } else {
                        node = node.child((byte) (0xf0 | (codePoint >> 18)));
                        if (node == null) {
                            return null;
                        }
                        node = node.child((byte) (0x80 | ((codePoint >> 12) & 0x3f)));
                        if (node == null) {
                            return null;
                        }
                        node = node.child((byte) (0x80 | ((codePoint >> 6) & 0x3f)));
                        if (node == null) {
                            return null;
                        }
                        node = node.child((byte) (0x80 | (codePoint & 0x3f)));
                    }
                    i += 6;
                } else {
                    // dealing with characters with values between 2048 and 65536 which
                    // equals to 2^16 or 16 bits, which is 3 bytes of data in UTF-8 encoding
                    node = node.child((byte) (0xe0 | (unicodeHexBytesAsChar >> 12)));
                    if (node == null) {
                        return null;
                    }
                    node = node.child((byte) (0x80 | ((unicodeHexBytesAsChar >> 6) & 0x3f)));
                    if (node == null) {
                        return null;
                    }
                    node = node.child((byte) (0x80 | (unicodeHexBytesAsChar & 0x3f)));
                }
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
     * Traverses the trie along the passed JSONPath segment starting from {@code begin} node.
     * The passed segment is represented as a key {@code (keyOffset, keyLength)} reference in {@code bytes} array.
     *
     * @param bytes     the message bytes.
     * @param begin     a TrieNode from which the traversal begins.
     * @param keyOffset the offset in {@code bytes} of the segment.
     * @param keyLength the length of the segment.
     * @return a TrieNode of the last symbol of the segment. {@code null} if the segment is not in the trie.
     */
    @Nullable
    TrieNode traverseJsonPathSegment(byte[] bytes, @Nullable final TrieNode begin, int keyOffset, int keyLength) {
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
     * A node in the Trie, represents part of the character (if character is ASCII, then represents a single character).
     * A padding of 128 is used to store references to the next positive and negative bytes (which range from -128 to
     * 128, hence the padding).
     */
    static class TrieNode {
        final TrieNode[] children = new TrieNode[256];
        /**
         * A marker that the character indicates that the key ends at this node.
         */
        boolean endOfWord = false;
        /**
         * Masking configuration for the key that ends at this node.
         */
        @Nullable
        KeyMaskingConfig keyMaskingConfig = null;
        /**
         * Used to store the configuration, but indicate that json-masker is in ALLOW mode and the key is not allowed.
         */
        boolean negativeMatch = false;

        /**
         * Retrieves a child node by the byte value. Returns {@code null}, if the trie has no matches.
         */
        @Nullable
        TrieNode child(byte b) {
            return children[b + BYTE_OFFSET];
        }

        /**
         * Adds a new child to the trie.
         */
        void add(byte b, TrieNode child) {
            children[b + BYTE_OFFSET] = child;
        }
    }
}
