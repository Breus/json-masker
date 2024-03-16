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

        Assertions.assertThat(maskingState.toString()).isEqualTo("""
                >{<
                    "mask
                """.stripTrailing());

        maskingState.incrementCurrentIndex(10);

        Assertions.assertThat(maskingState.toString()).isEqualTo("""
                {
                    "mas>k<Me": "some
                """.stripTrailing());

        maskingState.incrementCurrentIndex(20);

        Assertions.assertThat(maskingState.toString()).isEqualTo("""
                e value"
                }>
                """);

        maskingState.incrementCurrentIndex(1);

        Assertions.assertThat(maskingState.toString()).isEqualTo("""
                 value"
                }
                ><end of json>
                """.stripTrailing());
    }
}