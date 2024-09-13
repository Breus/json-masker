package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class MultiJsonTest {

    @Test
    void shouldMaskJsonLinesObjects() {
        JsonMasker jsonMasker = JsonMasker.getMasker(Set.of("maskMe"));

        Assertions.assertThat(jsonMasker.mask(
                        """
                                {"maskMe":"secret"}
                                {"maskMe":"secret"}
                                {"maskMe":"secret"}
                                """))
                .isEqualTo(
                        """
                                {"maskMe":"***"}
                                {"maskMe":"***"}
                                {"maskMe":"***"}
                                """);
    }

    @Test
    void shouldMaskJsonLinesArrays() {
        JsonMasker jsonMasker = JsonMasker.getMasker(Set.of("maskMe"));

        Assertions.assertThat(jsonMasker.mask(
                        """
                                [{"maskMe":"secret"}]
                                [{"maskMe":"secret"}]
                                [{"maskMe":"secret"}]
                                """))
                .isEqualTo(
                        """
                                [{"maskMe":"***"}]
                                [{"maskMe":"***"}]
                                [{"maskMe":"***"}]
                                """);
    }

    @Test
    void shouldMaskJsonLinesMixedTypes() {
        JsonMasker jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder().allowKeys().build());

        Assertions.assertThat(jsonMasker.mask(
                        """
                                "secret"
                                true
                                false
                                null
                                123
                                """))
                .isEqualTo(
                        """
                                "***"
                                "&&&"
                                "&&&"
                                null
                                "###"
                                """);
    }
    @Test
    void shouldMaskRepeatedJsonsWithoutNewLines() {
        JsonMasker jsonMasker = JsonMasker.getMasker(Set.of("maskMe"));

        Assertions.assertThat(jsonMasker.mask(
                        """
                                {"maskMe":"secret"}{"maskMe":"secret"}   {"maskMe":"secret"}
                                """))
                .isEqualTo(
                        """
                                {"maskMe":"***"}{"maskMe":"***"}   {"maskMe":"***"}
                                """);
    }
}
