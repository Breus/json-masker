package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;
import dev.blaauwendraad.masker.json.util.ByteValueMaskerContext;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

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
        KeyMatcher keyMatcher = new KeyMatcher(JsonMaskingConfig.builder().maskKeys("maskMe").build());
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
                .maskKeys("maskMe")
                .maskKeys("maskMeLikeCIA", KeyMaskingConfig.builder().maskStringsWith("[redacted]").build())
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
                .allowKeys("allowMe")
                .maskKeys("maskMeLikeCIA", KeyMaskingConfig.builder().maskStringsWith("[redacted]").build())
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
    void shouldNotMatchPrefix() {
        KeyMatcher keyMatcher = new KeyMatcher(JsonMaskingConfig.builder().maskKeys(Set.of("maskMe", "test")).build());
        assertThatConfig(keyMatcher, "mask").isNull();
        assertThatConfig(keyMatcher, "maskMe").isNotNull();
    }

    @Test
    void shouldAllowVeryLargeKeys() {
        String key = "k".repeat(10000);
        KeyMatcher keyMatcher = new KeyMatcher(JsonMaskingConfig.builder().maskKeys(Set.of(key)).build());
        assertThatConfig(keyMatcher, key).isNotNull();
    }

    private ObjectAssert<KeyMaskingConfig> assertThatConfig(KeyMatcher keyMatcher, String key) {
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        return Assertions.assertThat(keyMatcher.getMaskConfigIfMatched(bytes, 0, bytes.length, null));
    }

    @Test
    void printsNicely() {
        JsonMaskingConfig config = JsonMaskingConfig.builder()
                .allowKeys("romane", "romanus", "romulus", "rubens", "ruber", "rubicon", "rubicondus")
                .build();
        KeyMatcher keyMatcher = new KeyMatcher(config);
        assertThat(keyMatcher.printTree())
                .isEqualTo("""
                        r -> om -> an -> e
                                      -> us
                                -> ulus
                          -> ub -> e -> ns
                                     -> r
                                -> icon -> dus
                        """);
    }

    @Test
    void printsEmpty() {
        JsonMaskingConfig config = JsonMaskingConfig.builder()
                .allowKeys()
                .build();
        KeyMatcher keyMatcher = new KeyMatcher(config);
        assertThat(keyMatcher.printTree()).isEqualTo("\n");
    }

    @Test
    void compressPreInitTrie() {
        // Given, the target keys "breus" and "bruce"
        KeyMatcher keyMatcher = new KeyMatcher(JsonMaskingConfig.builder().maskKeys("breus", "bruce").build());

        KeyMatcher.PreInitTrieNode preInitTrieNode = new KeyMatcher.PreInitTrieNode();
        keyMatcher.insert(preInitTrieNode, "breus", false);
        keyMatcher.insert(preInitTrieNode, "bruce", false);

        // When
        KeyMatcher.RadixTrieNode trieNode = KeyMatcher.compress(preInitTrieNode);

        // Then, should become the following compressed radix Trie:
        // br -> eus
        //    -> uce
        assertThat(trieNode.prefixLowercase).isEqualTo("br".getBytes(StandardCharsets.UTF_8));
        assertThat(trieNode.prefixUppercase).isEqualTo("BR".getBytes(StandardCharsets.UTF_8));
        // Prefix index = 2, because b = 0, r = 1, and 2 would be 'e', but it will be looked-up in the next node because prefixIndex == currentRadixNode.length (2)
        assertThat(trieNode.child((byte) 'e', 2)).isNotNull().extracting("prefixLowercase").isEqualTo("us".getBytes(StandardCharsets.UTF_8));
        assertThat(trieNode.child((byte) 'u', 2)).isNotNull().extracting("prefixLowercase").isEqualTo("ce".getBytes(StandardCharsets.UTF_8));
    }


    @ParameterizedTest
    @MethodSource("encodedCharacterInputs")
    void isUnicodeEncodedCharacter(byte[] bytes, int fromIndex, int toIndex, boolean isEncodedCharacter) {
        assertThat(KeyMatcher.isUnicodeEncodedCharacter(bytes, fromIndex, toIndex)).isEqualTo(isEncodedCharacter);
    }

    private static Stream<Arguments> encodedCharacterInputs() {
        return Stream.of(
                Arguments.of(new byte[]{}, 0, 0, false),
                Arguments.of("hello\\u1234there".getBytes(StandardCharsets.UTF_8), 5, 11, true),
                Arguments.of("ሴ".getBytes(StandardCharsets.UTF_8), 0, 3, false),
                Arguments.of("\\u1234".getBytes(StandardCharsets.UTF_8), 0, 6, true)
        );
    }

    @Test
    void convertToRadixTrieNode() {
        // Given, the target keys "breus" and "bruce"
        KeyMatcher keyMatcher = new KeyMatcher(JsonMaskingConfig.builder().maskKeys("breus", "bruce").build());
        KeyMatcher.PreInitTrieNode preInitTrieNode = new KeyMatcher.PreInitTrieNode();
        keyMatcher.insert(preInitTrieNode, "breus", false);
        keyMatcher.insert(preInitTrieNode, "bruce", false);

        KeyMatcher.RadixTrieNode radixTrieNode = KeyMatcher.convertToRadixTrieNode(preInitTrieNode, List.of());
        // There is just one lower and uppercase child, representing 'br' and 'BR'
        assertThat(radixTrieNode.childrenLowercase.length).isEqualTo(1);
        assertThat(radixTrieNode.childrenUppercase.length).isEqualTo(1);
        KeyMatcher.RadixTrieNode childrenLowercase = radixTrieNode.childrenLowercase[0];
        Objects.requireNonNull(childrenLowercase);
        assertThat(childrenLowercase.prefixLowercase).isEqualTo("r".getBytes(StandardCharsets.UTF_8));

        // The offset of the children array is 101, because the first child is 'e' (101 in ASCII)
        assertThat(childrenLowercase.childrenLowercaseArrayOffset).isEqualTo(101);

        // The difference between 'e' (101 in ASCII) and 'u' (117 in ASCII) is 16, so 'e' is at index 0 and 'u' at index
        // 16 in the children array
        assertThat(childrenLowercase.childrenLowercase.length).isEqualTo(17);

        // This is the 'e' in 'breus' with as prefix 'us'
        KeyMatcher.RadixTrieNode childrenLowerCaseIndex0 = childrenLowercase.childrenLowercase[0];
        Objects.requireNonNull(childrenLowerCaseIndex0);
        assertThat(childrenLowerCaseIndex0.prefixLowercase).isEqualTo("us".getBytes(StandardCharsets.UTF_8));

        // This is the 'u' in 'bruce' with as prefix 'ce'
        KeyMatcher.RadixTrieNode childrenLowerCaseIndex16 = childrenLowercase.childrenLowercase[16];
        Objects.requireNonNull(childrenLowerCaseIndex16);
        assertThat(childrenLowerCaseIndex16.prefixLowercase).isEqualTo("ce".getBytes(StandardCharsets.UTF_8));
    }
}
