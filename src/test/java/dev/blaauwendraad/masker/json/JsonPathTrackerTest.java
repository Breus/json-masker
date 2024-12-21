package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;
import dev.blaauwendraad.masker.json.util.ByteValueMaskerContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JsonPathTrackerTest {

    @Test
    void jsonPathExceedsCapacity() {
        byte[] wildcard = "key".getBytes(StandardCharsets.UTF_8);
        KeyMatcher keyMatcher = new KeyMatcher(JsonMaskingConfig.builder().maskJsonPaths("$" + ".*".repeat(202)).build());
        JsonPathTracker jsonPathTracker = new JsonPathTracker(keyMatcher);
        for (int i = 0; i < 101; i++) {
            jsonPathTracker.pushKeyValueSegment(wildcard, 0, wildcard.length);
            Assertions.assertThat(jsonPathTracker.currentNode()).isNotNull();
        }
        for (int i = 0; i < 101; i++) {
            jsonPathTracker.pushArraySegment();
            Assertions.assertThat(jsonPathTracker.currentNode()).isNotNull();
        }
        for (int i = 0; i < 202; i++) {
            jsonPathTracker.backtrack();
            Assertions.assertThat(jsonPathTracker.currentNode()).isNotNull();
        }
        // backtracking last time from root
        jsonPathTracker.backtrack();
        Assertions.assertThat(jsonPathTracker.currentNode()).isNull();
    }

    @Test
    void shouldMatchJsonPaths() {
        KeyMatcher keyMatcher = new KeyMatcher(JsonMaskingConfig.builder().maskJsonPaths("$.a.b").build());
        JsonPathTracker jsonPathTracker = new JsonPathTracker(keyMatcher);

        String json = """
                {"a":{"b":1,"c":2}}
                """;
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        jsonPathTracker.pushKeyValueSegment(bytes, indexOf(bytes, 'a'), 1);
        jsonPathTracker.pushKeyValueSegment(bytes, indexOf(bytes, 'b'), 1);
        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, 0, jsonPathTracker.currentNode())).isNotNull();
        jsonPathTracker.backtrack();
        jsonPathTracker.backtrack();

        jsonPathTracker.pushKeyValueSegment(bytes, indexOf(bytes, 'a'), 1);
        jsonPathTracker.pushKeyValueSegment(bytes, indexOf(bytes, 'c'), 1);
        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, 0, jsonPathTracker.currentNode())).isNull();
        jsonPathTracker.backtrack();
        jsonPathTracker.backtrack();
    }

    @Test
    void shouldMatchJsonPathArrays() {
        KeyMatcher keyMatcher = new KeyMatcher(JsonMaskingConfig.builder().maskJsonPaths(Set.of("$.a[*].b", "$.a[*].c")).build());
        JsonPathTracker jsonPathTracker = new JsonPathTracker(keyMatcher);

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

        jsonPathTracker.pushKeyValueSegment(bytes, indexOf(bytes, 'a'), 1);
        jsonPathTracker.pushKeyValueSegment(bytes, -1, -1);
        jsonPathTracker.pushKeyValueSegment(bytes, indexOf(bytes, 'b'), 1);
        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, -1, jsonPathTracker.currentNode())).isNotNull();
        jsonPathTracker.backtrack();
        jsonPathTracker.backtrack();
        jsonPathTracker.backtrack();

        jsonPathTracker.pushKeyValueSegment(bytes, indexOf(bytes, 'a'), 1);
        jsonPathTracker.pushKeyValueSegment(bytes, -1, -1);
        jsonPathTracker.pushKeyValueSegment(bytes, indexOf(bytes, 'c'), 1);
        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, -1, jsonPathTracker.currentNode())).isNotNull();
        jsonPathTracker.backtrack();
        jsonPathTracker.backtrack();
        jsonPathTracker.backtrack();

        jsonPathTracker.pushKeyValueSegment(bytes, indexOf(bytes, 'a'), 1);
        jsonPathTracker.pushKeyValueSegment(bytes, -1, -1);
        jsonPathTracker.pushKeyValueSegment(bytes, indexOf(bytes, 'd'), 1);
        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, -1, jsonPathTracker.currentNode())).isNull();
        jsonPathTracker.backtrack();
        jsonPathTracker.backtrack();
        jsonPathTracker.backtrack();
    }

    @Test
    void shouldNotMatchJsonPathPrefix() {
        KeyMatcher keyMatcher = new KeyMatcher(JsonMaskingConfig.builder().maskJsonPaths("$.maskMe").build());
        JsonPathTracker jsonPathTracker = new JsonPathTracker(keyMatcher);

        String json = """
                {"maskMe":"secret"}
                """;
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        jsonPathTracker.pushKeyValueSegment(bytes, 2, 4);
        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, -1, jsonPathTracker.currentNode())).isNull();
        jsonPathTracker.backtrack();

        jsonPathTracker.pushKeyValueSegment(bytes, 2, 6);
        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, -1, jsonPathTracker.currentNode())).isNotNull();
        jsonPathTracker.backtrack();
    }

    @Test
    void shouldReturnMaskingConfigForJsonPathInAllowMode() {
        JsonMaskingConfig config = JsonMaskingConfig.builder()
                .allowJsonPaths("$.allowMe")
                .maskJsonPaths("$.maskMeLikeCIA", KeyMaskingConfig.builder().maskStringsWith("[redacted]").build())
                .build();
        KeyMatcher keyMatcher = new KeyMatcher(config);
        JsonPathTracker jsonPathTracker = new JsonPathTracker(keyMatcher);

        var json = """
                {"allowMe":"value","maskMe":"secret","maskMeLikeCIA":"secret"}
                """;
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        jsonPathTracker.pushKeyValueSegment(bytes, 2, 7);
        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, -1, jsonPathTracker.currentNode())).isNull();
        jsonPathTracker.backtrack();

        jsonPathTracker.pushKeyValueSegment(bytes, 20, 6);
        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, -1, jsonPathTracker.currentNode()))
                .isNotNull()
                .extracting(KeyMaskingConfig::getStringValueMasker)
                .extracting(masker -> ByteValueMaskerContext.maskStringWith("value", masker))
                .isEqualTo("\"***\"");
        jsonPathTracker.backtrack();

        jsonPathTracker.pushKeyValueSegment(bytes, 38, 13);
        assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, -1, jsonPathTracker.currentNode()))
                .isNotNull()
                .extracting(KeyMaskingConfig::getStringValueMasker)
                .extracting(masker -> ByteValueMaskerContext.maskStringWith("value", masker))
                .isEqualTo("\"[redacted]\"");
        jsonPathTracker.backtrack();
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