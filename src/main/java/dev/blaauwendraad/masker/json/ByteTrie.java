package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * This Trie structure is used as look-up optimization for JSON keys in the target key set.
 * <p>
 * <p>
 * The main idea is that we need to know whether a JSON key is in the target key set, one could do a contains on
 * the hash set, which would compute a hashcode for the whole key before doing a "fast" lookup. Another option would be
 * to iterate over target keys and compare characters one by one for each key, given that in reality most keys would
 * fail fast (assuming nobody asks us to mask keys {@code 'a[...]b'} in JSONs with keys {@code 'aa[...]b'})
 *
 * <p>
 * Both options are not ideal because
 * we:
 * <ul>
 *   <li>we expect set of target keys to be small</li>
 *   <li>we expect target keys themselves to also be small</li>
 *   <li>keys are case-insensitive by default, meaning that we have to do toLowerCase for every incoming key</li>
 * </ul>
 *
 * <p> For masking, we only care whether the key matched or not, so we can use a Trie structure to optimize the look-ups.
 * <p> Further we can construct a Trie that is case-insensitive, so that for we can avoid any transformations on
 * the incoming keys.
 * <p> We can also make a Trie that looks at bytes instead of characters, so that we can use the bytes and offsets
 * directly in the incoming JSON for comparison and make sure there are no allocations at all.
 * <p> And lastly, we can make a small optimization to remember all the distinct lengths of the target keys, so that
 * we can fail fast if the incoming key is not of the same length.
 */
@ParametersAreNonnullByDefault
final class ByteTrie {
    private static final int BYTE_OFFSET = -1 * Byte.MIN_VALUE;
    private final JsonMaskingConfig config;
    private final TrieNode root;
    private final boolean[] knownByteLengths = new boolean[256]; // byte can be anywhere between 0 and 256 length

    public ByteTrie(JsonMaskingConfig config) {
        this.config = config;
        this.root = new TrieNode();
    }

    /**
     * Inserts a word into the trie.
     *
     * @param word             the word to insert.
     * @param keyMaskingConfig the masking configuration for the key.
     * @param negativeMatch    if true, the key is not allowed and the trie is in ALLOW mode.
     *                         for example config
     *                         {@code
     *                         builder.allow("name", "age").mask("ssn", k -> k.maskStringsWith("[redacted]"))
     *                         }
     *                         would only allow {@code name} and {@code age} to be present in the JSON, it would use
     *                         default configuration to mask any other key, but would specifically mask {@code ssn} with
     *                         a string "[redacted]". To make it possible to store just the masking configuration we
     *                         insert a "negative match" node, that would not be treated as a target key, but provide
     *                         a fast lookup for the configuration
     */
    public void insert(String word, KeyMaskingConfig keyMaskingConfig, boolean negativeMatch) {
        boolean caseInsensitive = !config.caseSensitiveTargetKeys();
        byte[] bytes = word.getBytes(StandardCharsets.UTF_8);
        knownByteLengths[bytes.length] = true;
        byte[] lowerBytes = null;
        byte[] upperBytes = null;
        if (caseInsensitive) {
            /*
             from inspecting the code, it looks like lower casing a character does not change the byte length
             on the same encoding, however the documentation explicitly mentions that resulting length might be
             different
             so better to fail fast if instead of ignoring that.

             Given that we're doing that only for target keys, the idea that it's going to have different lengths
             is quite unlikely
            */
            lowerBytes = word.toLowerCase().getBytes(StandardCharsets.UTF_8);
            upperBytes = word.toUpperCase().getBytes(StandardCharsets.UTF_8);

            if (bytes.length != lowerBytes.length || bytes.length != upperBytes.length) {
                throw new IllegalArgumentException("Case insensitive trie does not support all characters");
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
        node.keyMaskingConfig = keyMaskingConfig;
        node.endOfWord = true;
        node.negativeMatch = negativeMatch;
    }

    /**
     * If the key matches - returns its masking configuration, otherwise returns null.
     */
    public boolean search(byte[] bytes, int offset, int length) {
        TrieNode node = searchNode(bytes, offset, length);
        return node != null && !node.negativeMatch;
    }

    /**
     * Must be called only if the key has matched or in allow mode, for key that did not match
     * returns its masking configuration, otherwise returns default config.
     */
    public KeyMaskingConfig config(byte[] bytes, int offset, int length) {
        TrieNode node = searchNode(bytes, offset, length);
        // in allow mode we explicitly forbid requesting configs for allowed keys
        // this would not make sense and likely means error in the logic
        // though for mask mode we cannot make the same assumption, as we might mask any
        // arbitrary key with a specific config, if that key is part of the object being masked
        if (config.isInAllowMode()) {
            if (node != null && !node.negativeMatch) {
                throw new IllegalArgumentException("getting masking config for allowed key in ALLOW mode is not allowed");
            }
        }
        return node != null ? node.keyMaskingConfig : config.getDefaultConfig();
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

    /**
     * Returns whether the json path is in the trie.
     * JsonPath is represented as an iterator over references of the json path segments in the byte array.
     * A reference is represented as a (keyStartIndex, keyLength) pair.
     *
     * @param bytes    byte array representation fo the json
     * @param jsonPath an iterator over references of json path segments in <code>bytes</code>.
     * @return true if the json path key is in the trie, false otherwise.
     */
    public boolean searchForJsonPathKey(byte[] bytes, Iterator<MaskingState.SegmentReference> jsonPath) {
        TrieNode node = searchForJsonPathKeyNode(bytes, jsonPath);
        return node != null && !node.negativeMatch;
    }

    private TrieNode searchForJsonPathKeyNode(byte[] bytes, Iterator<MaskingState.SegmentReference> jsonPath) {
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
            MaskingState.SegmentReference segmentReference = jsonPath.next();
            int keyStartIndex = segmentReference.start;
            int keyLength = segmentReference.offset;
            for (int i = keyStartIndex; i < keyStartIndex + keyLength; i++) {
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
