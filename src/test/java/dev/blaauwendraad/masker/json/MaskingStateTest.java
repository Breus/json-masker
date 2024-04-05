package dev.blaauwendraad.masker.json;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

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
            maskingState.expandCurrentJsonPath(new KeyMatcher.TrieNode());
        }
        Assertions.assertThat(maskingState.getCurrentJsonPathNode()).isNotNull();
    }

    @Test
    void getCurrentJsonPathNodeFromEmptyJsonPath() {
        MaskingState maskingState = new MaskingState("[]".getBytes(StandardCharsets.UTF_8), true);
        Assertions.assertThat(maskingState.getCurrentJsonPathNode()).isNull();
    }
}