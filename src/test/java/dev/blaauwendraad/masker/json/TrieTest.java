package dev.blaauwendraad.masker.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Locale;
import java.util.Set;

@ParametersAreNonnullByDefault
final class TrieTest {
    private static final Set<String> someKeys = Set.of("maskMe", "maskme", "\u000F\u0017\u0017\u000Bs\b\u0014X");

    @Test
    void caseInsensitiveInsertAndSearch() {
        Trie trie = new Trie(true);
        for (String someKey : someKeys) {
            trie.insert(someKey);
        }
        for (String someKey : someKeys) {
            Assertions.assertTrue(trie.search(someKey));
        }
        for (String someKey : someKeys) {
            Assertions.assertTrue(trie.search(someKey.toLowerCase(Locale.ROOT)));
        }
        for (String someKey : someKeys) {
            Assertions.assertTrue(trie.search(someKey.toUpperCase(Locale.ROOT)));
        }
        Assertions.assertFalse(trie.search("notAKey"));
    }

    @Test
    void caseSensitiveInsertAndSearch() {
        Trie trie = new Trie(false);
        for (String someKey : someKeys) {
            trie.insert(someKey);
        }
        for (String someKey : someKeys) {
            Assertions.assertTrue(trie.search(someKey));
        }
        for (String someKey : someKeys) {
            Assertions.assertFalse(trie.search(someKey.toUpperCase(Locale.ROOT)));
        }
        Assertions.assertFalse(trie.search("notAKey"));
    }
}
