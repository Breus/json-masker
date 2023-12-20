package dev.blaauwendraad.masker.json;

import java.nio.charset.StandardCharsets;

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
final class ByteTrie {
    private static final int MAX_BYTE_SIZE = 128;
    private final TrieNode root;
    private final boolean caseInsensitive;
    private final boolean[] knownByteLengths = new boolean[256]; // byte can be anywhere between 0 and 256 length

    public ByteTrie(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
        this.root = new TrieNode();
    }

    /**
     * Inserts a word into the trie.
     */
    public void insert(String word) {
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
            TrieNode child = node.children[b + MAX_BYTE_SIZE];
            if (child == null) {
                child = new TrieNode();
                node.children[b + MAX_BYTE_SIZE] = child;
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
                    node.children[lowerBytes[i] + MAX_BYTE_SIZE] = child;
                    node.children[upperBytes[i] + MAX_BYTE_SIZE] = child;
                }
            }
            node = child;
        }
        node.endOfWord = true;
    }

    /**
     * Returns if the word is in the trie.
     */
    public boolean search(byte[] bytes, int offset, int length) {
        if (!knownByteLengths[length]) {
            return false;
        }
        TrieNode node = root;

        for (int i = offset; i < offset + length; i++) {
            int b = bytes[i];
            node = node.children[b + MAX_BYTE_SIZE];

            if (node == null) {
                return false;
            }
        }

        return node.endOfWord;
    }

    /**
     * A node in the Trie, represents part of the character (if character is ASCII, then represents a single character).
     * A padding of 128 is used to store references to the next positive and negative bytes (which range from -128 to
     * 128, hence the padding).
     */
    private static class TrieNode {
        private final TrieNode[] children = new TrieNode[256];
        private boolean endOfWord = false;
    }
}
