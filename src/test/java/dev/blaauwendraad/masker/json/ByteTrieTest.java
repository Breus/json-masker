package dev.blaauwendraad.masker.json;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

final class ByteTrieTest {
    private static final Set<String> someKeys = Set.of("maskMe", "maskme", "\u000F\u0017\u0017\u000Bs\b\u0014X");

    @Test
    void caseInsensitiveInsertAndSearch() {
        ByteTrie trie = new ByteTrie(true);
        for (String someKey : someKeys) {
            trie.insert(someKey);
        }
        for (String someKey : someKeys) {
            assertThat(transformToBytesAndSearch(trie, someKey)).isTrue();
            assertThat(transformToBytesWithPaddingAndSearch(trie, someKey)).isTrue();
        }
        for (String someKey : someKeys) {
            assertThat(transformToBytesAndSearch(trie, someKey.toLowerCase(Locale.ROOT))).isTrue();
            assertThat(transformToBytesWithPaddingAndSearch(trie, someKey.toLowerCase(Locale.ROOT))).isTrue();
        }
        for (String someKey : someKeys) {
            assertThat(transformToBytesAndSearch(trie, someKey.toUpperCase(Locale.ROOT))).isTrue();
            assertThat(transformToBytesWithPaddingAndSearch(trie, someKey.toUpperCase(Locale.ROOT))).isTrue();
        }
        assertThat(transformToBytesAndSearch(trie, "notAKey")).isFalse();
        assertThat(transformToBytesWithPaddingAndSearch(trie, "notAKey")).isFalse();
    }

    @Test
    void caseSensitiveInsertAndSearch() {
        ByteTrie trie = new ByteTrie(false);
        for (String someKey : someKeys) {
            trie.insert(someKey);
        }
        for (String someKey : someKeys) {
            assertThat(transformToBytesAndSearch(trie, someKey)).isTrue();
            assertThat(transformToBytesWithPaddingAndSearch(trie, someKey)).isTrue();
        }
        for (String someKey : someKeys) {
            assertThat(transformToBytesAndSearch(trie, someKey.toUpperCase(Locale.ROOT))).isFalse();
            assertThat(transformToBytesWithPaddingAndSearch(trie, someKey.toUpperCase(Locale.ROOT))).isFalse();
        }
        assertThat(transformToBytesAndSearch(trie, "notAKey")).isFalse();
        assertThat(transformToBytesWithPaddingAndSearch(trie, "notAKey")).isFalse();
    }

    private boolean transformToBytesAndSearch(ByteTrie trie, String key) {
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        return trie.search(bytes, 0, bytes.length);
    }

    private boolean transformToBytesWithPaddingAndSearch(ByteTrie trie, String key) {
        // searching by offsets
        byte[] bytes = ("{\"" + key + "\": \"some value\"}\"").getBytes(StandardCharsets.UTF_8);
        return trie.search(bytes, 2, key.getBytes(StandardCharsets.UTF_8).length);
    }
}
