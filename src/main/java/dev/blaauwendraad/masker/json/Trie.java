package dev.blaauwendraad.masker.json;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

/**
 * This Trie structure is used as look-up optimization for JSON keys in the target key set.
 * <p>
 * The main benefit for this is because by default target keys are considered case insensitive.
 * The main benefit of this is that instead of allocating a new toLowerCase String on the heap which is looked-up in the
 * lowerCase set of target keys, a single (lowercase) char is allocated on the stack and matched against the target key
 * set, one by one. It will either fail fast as soon as the first character doesn't match, or return true.
 */
public class Trie {
    private final TrieNode root;
    private final boolean caseInsensitive;

    public Trie(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
        this.root = new TrieNode();
    }

    /**
     * Inserts a word into the trie.
     */
    public void insert(String word) {
        TrieNode node = root;
        for (int i = 0; i < word.length(); i++) {
            char character = word.charAt(i);
            if (caseInsensitive) {
                character = Character.toLowerCase(character);
            }
            node = node.children.computeIfAbsent(character, key -> new TrieNode());
        }
        node.endOfWord = true;
    }

    /**
     * Returns if the word is in the trie.
     */
    public boolean search(String word) {
        TrieNode node = root;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (caseInsensitive) {
                c = Character.toLowerCase(c);
            }

            node = node.children.get(c);

            if (node == null) {
                return false;
            }
        }

        return node.endOfWord;
    }

    /**
     * A node in the Trie
     */
    public static class TrieNode {
        private final Map<Character, TrieNode> children = new HashMap<>();
        private boolean endOfWord = false;
    }
}
