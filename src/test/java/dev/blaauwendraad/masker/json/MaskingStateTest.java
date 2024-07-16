package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Set;

class MaskingStateTest {
    @Test
    void shouldReturnStringRepresentationForDebugging() {
        MaskingState maskingState = new MaskingState("""
                {
                    "maskMe": "some value"
                }
                """.getBytes(StandardCharsets.UTF_8), false);

        Assertions.assertThat(maskingState).hasToString("""
                >{<
                    "mask
                """.stripTrailing());

        maskingState.incrementIndex(10);

        Assertions.assertThat(maskingState).hasToString("""
                {
                    "mas>k<Me": "some
                """.stripTrailing());

        maskingState.incrementIndex(20);

        Assertions.assertThat(maskingState).hasToString("""
                e value"
                }>
                """);

        maskingState.incrementIndex(1);

        Assertions.assertThat(maskingState).hasToString("""
                 value"
                }
                ><end of json>
                """.stripTrailing());
    }

    @Test
    void jsonPathExceedsCapacity() {
        MaskingState maskingState = new MaskingState("[]".getBytes(StandardCharsets.UTF_8), true);
        for (int i = 0; i < 101; i++) {
            maskingState.expandCurrentJsonPath(KeyMatcher.transform(new KeyMatcher.PreInitTrieNode()));
        }
        Assertions.assertThat(maskingState.getCurrentJsonPathNode()).isNotNull();
    }

    @Test
    void getCurrentJsonPathNodeFromEmptyJsonPath() {
        MaskingState maskingState = new MaskingState("[]".getBytes(StandardCharsets.UTF_8), true);
        Assertions.assertThat(maskingState.getCurrentJsonPathNode()).isNull();
    }

    @Test
    void shouldThrowErrorWhenGettingStartValueIndexOutsideOfMasking() {
        MaskingState maskingState = new MaskingState("""
                {
                    "maskMe": "some value"
                }
                """.getBytes(StandardCharsets.UTF_8), false);

        Assertions.assertThatThrownBy(() -> maskingState.getCurrentValueStartIndex())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldUseCorrectOffsetWhenThrowingValueMaskerError() {
        var jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .maskKeys("maskMe")
                        .maskStringsWith(context -> {
                            throw context.invalidJson("Didn't like the value at index 3", 3);
                        })
                .build()
        );

        Assertions.assertThatThrownBy(() ->
                jsonMasker.mask("""
                {
                    "maskMe": "some value"
                }
                """
        ))
                .isInstanceOf(InvalidJsonException.class)
                .hasMessage("Didn't like the value at index 3 at index 19");
    }
}