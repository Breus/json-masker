package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;
import org.junit.jupiter.api.Test;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ParametersAreNonnullByDefault
final class ByteTrieTest {
    private static final Set<String> someKeys = Set.of("maskMe", "maskme", "\u000F\u0017\u0017\u000Bs\b\u0014X");

    @Test
    void caseInsensitiveInsertAndSearch() {
        ByteTrie trie = new ByteTrie(JsonMaskingConfig.builder().maskKeys(someKeys).build());
        for (String someKey : someKeys) {
            trie.insert(someKey, false);
        }
        for (String someKey : someKeys) {
            assertThat(transformToBytesAndSearch(trie, someKey)).isTrue();
            assertThat(transformToBytesWithPaddingAndSearch(trie, someKey)).isTrue();
            assertThat(transformToBytesAndGetConfig(trie, someKey)).isNotNull();
        }
        for (String someKey : someKeys) {
            assertThat(transformToBytesAndSearch(trie, someKey.toLowerCase(Locale.ROOT))).isTrue();
            assertThat(transformToBytesWithPaddingAndSearch(trie, someKey.toLowerCase(Locale.ROOT))).isTrue();
            assertThat(transformToBytesAndGetConfig(trie, someKey.toLowerCase(Locale.ROOT))).isNotNull();
        }
        for (String someKey : someKeys) {
            assertThat(transformToBytesAndSearch(trie, someKey.toUpperCase(Locale.ROOT))).isTrue();
            assertThat(transformToBytesWithPaddingAndSearch(trie, someKey.toUpperCase(Locale.ROOT))).isTrue();
            assertThat(transformToBytesAndGetConfig(trie, someKey.toUpperCase(Locale.ROOT))).isNotNull();
        }
        assertThat(transformToBytesAndSearch(trie, "notAKey")).isFalse();
        assertThat(transformToBytesWithPaddingAndSearch(trie, "notAKey")).isFalse();
        assertThat(transformToBytesAndGetConfig(trie, "notAKey")).isNotNull();
    }

    @Test
    void caseSensitiveInsertAndSearch() {
        ByteTrie trie = new ByteTrie(JsonMaskingConfig.builder().maskKeys(someKeys).caseSensitiveTargetKeys().build());
        for (String someKey : someKeys) {
            trie.insert(someKey, false);
        }
        for (String someKey : someKeys) {
            assertThat(transformToBytesAndSearch(trie, someKey)).isTrue();
            assertThat(transformToBytesWithPaddingAndSearch(trie, someKey)).isTrue();
            assertThat(transformToBytesAndGetConfig(trie, someKey)).isNotNull();
        }
        for (String someKey : someKeys) {
            assertThat(transformToBytesAndSearch(trie, someKey.toUpperCase(Locale.ROOT))).isFalse();
            assertThat(transformToBytesWithPaddingAndSearch(trie, someKey.toUpperCase(Locale.ROOT))).isFalse();
            assertThat(transformToBytesAndGetConfig(trie, someKey.toUpperCase(Locale.ROOT))).isNotNull();
        }
        assertThat(transformToBytesAndSearch(trie, "notAKey")).isFalse();
        assertThat(transformToBytesWithPaddingAndSearch(trie, "notAKey")).isFalse();
        assertThat(transformToBytesAndGetConfig(trie, "notAKey")).isNotNull();
    }

    @Test
    void returnsConfigsForNegativeMatches() {
        JsonMaskingConfig config = JsonMaskingConfig.builder()
                .allowKeys("allowMe")
                .maskKeys("maskMe", k -> k.maskStringsWith("[redacted]"))
                .build();
        ByteTrie trie = new ByteTrie(config);

        trie.insert("allowMe", false);
        trie.insert("maskMe", true);

        assertThat(transformToBytesAndSearch(trie, "allowMe")).isTrue();
        assertThatThrownBy(() -> transformToBytesAndGetConfig(trie, "allowMe"));

        assertThat(transformToBytesAndSearch(trie, "maskMe")).isFalse();
        assertThat(transformToBytesAndGetConfig(trie, "maskMe"))
                .extracting(KeyMaskingConfig::getMaskStringsWith)
                .isEqualTo("[redacted]");

        assertThat(transformToBytesAndSearch(trie, "notAKey")).isFalse();
        assertThat(transformToBytesAndGetConfig(trie, "notAKey"))
                .extracting(KeyMaskingConfig::getMaskStringsWith)
                .isEqualTo("***");
    }

    private boolean transformToBytesAndSearch(ByteTrie trie, String key) {
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        return trie.search(bytes, 0, bytes.length);
    }

    private KeyMaskingConfig transformToBytesAndGetConfig(ByteTrie trie, String key) {
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        return trie.config(bytes, 0, bytes.length);
    }

    private boolean transformToBytesWithPaddingAndSearch(ByteTrie trie, String key) {
        // searching by offsets
        byte[] bytes = ("{\"" + key + "\": \"some value\"}\"").getBytes(StandardCharsets.UTF_8);
        return trie.search(bytes, 2, key.getBytes(StandardCharsets.UTF_8).length);
    }
}
