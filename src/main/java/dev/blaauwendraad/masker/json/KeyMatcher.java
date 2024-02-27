package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;

import javax.annotation.CheckForNull;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

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
    private static final int DECIMAL_RADIX = 10;
    private static final int BYTE_OFFSET = -1 * Byte.MIN_VALUE;
    private static final int SKIP_KEY_LOOKUP = -1;
    private final JsonMaskingConfig maskingConfig;
    private final TrieNode root;
    private final boolean[] knownByteLengths = new boolean[256]; // byte can be anywhere between 0 and 256 length

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
    @SuppressWarnings("java:S2259") // sonar complains that lowerBytes can be null, but it can not
    private void insert(String word, boolean negativeMatch) {
        boolean caseInsensitive = !maskingConfig.caseSensitiveTargetKeys();
        byte[] bytes = word.getBytes(StandardCharsets.UTF_8);
        knownByteLengths[bytes.length] = true;
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
            TrieNode child = node.children[b + BYTE_OFFSET];
            if (child == null) {
                child = new TrieNode();
                node.children[b + BYTE_OFFSET] = child;
                if (caseInsensitive) {
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
                    node.children[lowerBytes[i] + BYTE_OFFSET] = child;
                    node.children[upperBytes[i] + BYTE_OFFSET] = child;
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
    @CheckForNull
    public KeyMaskingConfig getMaskConfigIfMatched(byte[] bytes, int keyOffset, int keyLength, Iterator<JsonPathSegmentReference> jsonPath) {
        // first search by key
        if (maskingConfig.isInMaskMode()) {
            // check json path first, as it's more specific
            TrieNode node = searchForJsonPathKeyNode(bytes, jsonPath);
            // if found - mask with this config
            // if not found - do not mask
            if (node != null && !node.negativeMatch) {
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
            // check json path first, as it's more specific
            TrieNode node = searchForJsonPathKeyNode(bytes, jsonPath);
            // if found and is not negativeMatch - do not mask
            // if found and is negative match - mask, but with a specific config
            // if not found - mask with default config
            if (node != null) {
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

    /**
     * An overload of {@code dev.blaauwendraad.masker.json.KeyMatcher#getMaskConfigIfMatched(byte[], int, int, java.util.Iterator)} specifically for jsonpath look-ups.
     */
    @CheckForNull
    public KeyMaskingConfig getMaskConfigIfMatched(byte[] bytes, Iterator<JsonPathSegmentReference> jsonPath) {
        return getMaskConfigIfMatched(bytes, 0, SKIP_KEY_LOOKUP, jsonPath);
    }

    @CheckForNull
    private TrieNode searchNode(byte[] bytes, int offset, int length) {
        if (!knownByteLengths[length]) {
            return null;
        }
        TrieNode node = root;

        for (int i = offset; i < offset + length; i++) {
            int b = bytes[i];
            node = node.children[b + BYTE_OFFSET];

            if (node == null) {
                return null;
            }
        }

        if (!node.endOfWord) {
            return null;
        }

        return node;
    }

    @CheckForNull
    private TrieNode searchForJsonPathKeyNode(byte[] bytes, Iterator<JsonPathSegmentReference> jsonPath) {
        TrieNode node = root;
        node = node.children['$' + BYTE_OFFSET];
        if (node == null) {
            return null;
        }
        while (jsonPath.hasNext()) {
            node = node.children['.' + BYTE_OFFSET];
            if (node == null) {
                return null;
            }
            JsonPathSegmentReference jsonPathSegmentReference = jsonPath.next();
            int keyOffset = jsonPathSegmentReference.getOffset();
            int keyLength = jsonPathSegmentReference.getLength();
            if (jsonPathSegmentReference.isArraySegment()) {
                // for arrays keyOffset is the index of the element
                if (keyOffset > 10) {
                    char[] digits = String.valueOf(keyOffset).toCharArray();
                    for (char digit : digits) {
                        node = node.children[digit + BYTE_OFFSET];
                        if (node == null) {
                            return null;
                        }
                    }
                } else {
                    char digit = Character.forDigit(keyOffset, DECIMAL_RADIX);
                    if (digit == '\u0000') {
                        throw new IllegalStateException("Invalid digit " + keyOffset);
                    }
                    node = node.children[digit + BYTE_OFFSET];
                    if (node == null) {
                        return null;
                    }
                }
                continue;
            }
            for (int i = keyOffset; i < keyOffset + keyLength; i++) {
                int b = bytes[i];
                node = node.children[b + BYTE_OFFSET];
                if (node == null) {
                    return null;
                }
            }
        }

        if (!node.endOfWord) {
            return null;
        }

        return node;
    }

    /**
     * A node in the Trie, represents part of the character (if character is ASCII, then represents a single character).
     * A padding of 128 is used to store references to the next positive and negative bytes (which range from -128 to
     * 128, hence the padding).
     */
    private static class TrieNode {
        private final TrieNode[] children = new TrieNode[256];
        /**
         * A marker that the character indicates that the key ends at this node.
         */
        private boolean endOfWord = false;
        /**
         * Masking configuration for the key that ends at this node.
         */
        @CheckForNull
        private KeyMaskingConfig keyMaskingConfig = null;
        /**
         * Used to store the configuration, but indicate that json-masker is in ALLOW mode and the key is not allowed.
         */
        private boolean negativeMatch = false;
    }
}
