package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;
import dev.blaauwendraad.masker.json.util.ByteValueMaskerContext;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

final class KeyMatcherTest {
    private static final Set<String> keys = Set.of("maskMe", "maskme", "\u000F\u0017\u0017\u000Bs\b\u0014X");

    @Test
    void shouldMatchKeysCaseInsensitiveByDefault() {
        KeyMatcher keyMatcher = new KeyMatcher(JsonMaskingConfig.builder().maskKeys(keys).build());
        for (String key : keys) {
            assertThatConfig(keyMatcher, key).isNotNull();
        }
        for (String key : keys) {
            assertThatConfig(keyMatcher, key.toLowerCase(Locale.ROOT)).isNotNull();
        }
        for (String key : keys) {
            assertThatConfig(keyMatcher, key.toUpperCase(Locale.ROOT)).isNotNull();
        }
        assertThatConfig(keyMatcher, "notAKey").isNull();
    }

    @Test
    void shouldMatchKeysCaseSensitiveIfSpecified() {
        KeyMatcher keyMatcher = new KeyMatcher(JsonMaskingConfig.builder()
                .maskKeys(keys)
                .caseSensitiveTargetKeys()
                .build()
        );
        for (String key : keys) {
            assertThatConfig(keyMatcher, key).isNotNull();
        }
        for (String key : keys) {
            assertThatConfig(keyMatcher, key.toUpperCase(Locale.ROOT)).isNull();
        }
        assertThatConfig(keyMatcher, "notAKey").isNull();
    }

    @Test
    void shouldBeAbleToSearchByOffset() {
        KeyMatcher keyMatcher = new KeyMatcher(JsonMaskingConfig.builder().maskKeys(Set.of("maskMe")).build());
        byte[] bytes = "maskMe".getBytes(StandardCharsets.UTF_8);
        byte[] bytesWithPadding = """
                {"maskMe": "secret"}
                """.strip().getBytes(StandardCharsets.UTF_8);

        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, bytes.length, null)).isNotNull();
        assertThat(keyMatcher.getMaskConfigIfMatched(bytesWithPadding, 2, bytes.length, null)).isNotNull();
    }

    @Test
    void shouldReturnSpecificConfigWhenMatched() {
        JsonMaskingConfig config = JsonMaskingConfig.builder()
                .maskKeys(Set.of("maskMe"))
                .maskKeys(Set.of("maskMeLikeCIA"), KeyMaskingConfig.builder().maskStringsWith("[redacted]").build())
                .build();
        KeyMatcher keyMatcher = new KeyMatcher(config);

        assertThatConfig(keyMatcher, "maskMe")
                .isNotNull()
                .extracting(KeyMaskingConfig::getStringValueMasker)
                .extracting(masker -> ByteValueMaskerContext.maskStringWith("value", masker))
                .isEqualTo("\"***\"");

        assertThatConfig(keyMatcher, "maskMeLikeCIA")
                .isNotNull()
                .extracting(KeyMaskingConfig::getStringValueMasker)
                .extracting(masker -> ByteValueMaskerContext.maskStringWith("value", masker))
                .isEqualTo("\"[redacted]\"");
    }

    @Test
    void shouldReturnMaskingConfigInAllowMode() {
        JsonMaskingConfig config = JsonMaskingConfig.builder()
                .allowKeys(Set.of("allowMe"))
                .maskKeys(Set.of("maskMeLikeCIA"), KeyMaskingConfig.builder().maskStringsWith("[redacted]").build())
                .build();
        KeyMatcher keyMatcher = new KeyMatcher(config);

        assertThatConfig(keyMatcher, "allowMe").isNull();

        assertThatConfig(keyMatcher, "maskMe")
                .isNotNull()
                .extracting(KeyMaskingConfig::getStringValueMasker)
                .extracting(masker -> ByteValueMaskerContext.maskStringWith("value", masker))
                .isEqualTo("\"***\"");

        assertThatConfig(keyMatcher, "maskMeLikeCIA")
                .isNotNull()
                .extracting(KeyMaskingConfig::getStringValueMasker)
                .extracting(masker -> ByteValueMaskerContext.maskStringWith("value", masker))
                .isEqualTo("\"[redacted]\"");
    }

    @Test
    void shouldMatchJsonPaths() {
        KeyMatcher keyMatcher = new KeyMatcher(JsonMaskingConfig.builder().maskJsonPaths(Set.of("$.a.b")).build());
        String json = """
                {"a":{"b":1,"c":2}}
                """;
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        KeyMatcher.TrieNode node = keyMatcher.getJsonPathRootNode();
        node = keyMatcher.traverseJsonPathSegment(bytes, node, indexOf(bytes, 'a'), 1);
        node = keyMatcher.traverseJsonPathSegment(bytes, node, indexOf(bytes, 'b'), 1);
        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, 0, node)).isNotNull();

        node = keyMatcher.getJsonPathRootNode();
        node = keyMatcher.traverseJsonPathSegment(bytes, node, indexOf(bytes, 'a'), 1);
        node = keyMatcher.traverseJsonPathSegment(bytes, node, indexOf(bytes, 'c'), 1);
        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, 0, node)).isNull();
    }

    @Test
    void shouldMatchJsonPathArrays() {
        KeyMatcher keyMatcher = new KeyMatcher(JsonMaskingConfig.builder().maskJsonPaths(Set.of("$.a[*].b", "$.a[*].c")).build());
        String json = """
                {
                  "a": [
                    0,
                    1,
                    2,
                    3,
                    4,
                    5,
                    6,
                    7,
                    8,
                    {
                      "b": 1
                    },
                    10,
                    {
                      "c": 1,
                      "d": 1
                    }
                  ]
                }
                """;
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        KeyMatcher.TrieNode node = keyMatcher.getJsonPathRootNode();
        node = keyMatcher.traverseJsonPathSegment(bytes, node, indexOf(bytes, 'a'), 1);
        node = keyMatcher.traverseJsonPathSegment(bytes, node, -1, -1);
        node = keyMatcher.traverseJsonPathSegment(bytes, node, indexOf(bytes, 'b'), 1);
        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, -1, node)).isNotNull();

        node = keyMatcher.getJsonPathRootNode();
        node = keyMatcher.traverseJsonPathSegment(bytes, node, indexOf(bytes, 'a'), 1);
        node = keyMatcher.traverseJsonPathSegment(bytes, node, -1, -1);
        node = keyMatcher.traverseJsonPathSegment(bytes, node, indexOf(bytes, 'c'), 1);
        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, -1, node)).isNotNull();

        node = keyMatcher.getJsonPathRootNode();
        node = keyMatcher.traverseJsonPathSegment(bytes, node, indexOf(bytes, 'a'), 1);
        node = keyMatcher.traverseJsonPathSegment(bytes, node, -1, -1);
        node = keyMatcher.traverseJsonPathSegment(bytes, node, indexOf(bytes, 'd'), 1);
        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, -1, node)).isNull();
    }

    @Test
    void shouldNotMatchPrefix() {
        KeyMatcher keyMatcher = new KeyMatcher(JsonMaskingConfig.builder().maskKeys(Set.of("maskMe", "test")).build());
        assertThatConfig(keyMatcher, "mask").isNull();
        assertThatConfig(keyMatcher, "maskMe").isNotNull();
    }

    @Test
    void shouldNotMatchJsonPathPrefix() {
        KeyMatcher keyMatcher = new KeyMatcher(JsonMaskingConfig.builder().maskJsonPaths(Set.of("$.maskMe")).build());
        String json = """
                {"maskMe":"secret"}
                """;
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        KeyMatcher.TrieNode node = keyMatcher.getJsonPathRootNode();
        node = keyMatcher.traverseJsonPathSegment(bytes, node, 2, 4);
        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, -1, node)).isNull();

        node = keyMatcher.getJsonPathRootNode();
        node = keyMatcher.traverseJsonPathSegment(bytes, node, 2, 6);
        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, -1, node)).isNotNull();
    }

    @Test
    void shouldReturnMaskingConfigForJsonPathInAllowMode() {
        JsonMaskingConfig config = JsonMaskingConfig.builder()
                .allowJsonPaths(Set.of("$.allowMe"))
                .maskJsonPaths(Set.of("$.maskMeLikeCIA"), KeyMaskingConfig.builder().maskStringsWith("[redacted]").build())
                .build();
        KeyMatcher keyMatcher = new KeyMatcher(config);

        var json = """
                {"allowMe":"value","maskMe":"secret","maskMeLikeCIA":"secret"}
                """;
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        KeyMatcher.TrieNode node = keyMatcher.getJsonPathRootNode();
        node = keyMatcher.traverseJsonPathSegment(bytes, node, 2, 7);
        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, -1, node)).isNull();

        node = keyMatcher.getJsonPathRootNode();
        node = keyMatcher.traverseJsonPathSegment(bytes, node, 20, 6);
        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, -1, node))
                .isNotNull()
                .extracting(KeyMaskingConfig::getStringValueMasker)
                .extracting(masker -> ByteValueMaskerContext.maskStringWith("value", masker))
                .isEqualTo("\"***\"");

        node = keyMatcher.getJsonPathRootNode();
        node = keyMatcher.traverseJsonPathSegment(bytes, node, 38, 13);
        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, -1, node))
                .isNotNull()
                .extracting(KeyMaskingConfig::getStringValueMasker)
                .extracting(masker -> ByteValueMaskerContext.maskStringWith("value", masker))
                .isEqualTo("\"[redacted]\"");
    }

    private ObjectAssert<KeyMaskingConfig> assertThatConfig(KeyMatcher keyMatcher, String key) {
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        return Assertions.assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, bytes.length, null));
    }

    // utility to find specific char in the array, must not be duplicated
    private int indexOf(byte[] bytes, char c) {
        int found = -1;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == (byte) c) {
                if (found != -1) {
                    throw new IllegalStateException("Char must not be duplicated, got on index %s and %s".formatted(found, i));
                }
                found = i;
            }
        }
        if (found == -1) {
            throw new IllegalStateException("Byte array must contain the char");
        }
        return found;
    }
}
