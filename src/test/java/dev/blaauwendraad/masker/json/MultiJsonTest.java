package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class MultiJsonTest {

    @Test
    void shouldMaskJsonLinesObjects() {
        JsonMasker jsonMasker = JsonMasker.getMasker(Set.of("maskMe"));

        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker,
                """
                        {"maskMe":"secret"}
                        {"maskMe":"secret"}
                        {"maskMe":"secret"}
                        """,
                """
                        {"maskMe":"***"}
                        {"maskMe":"***"}
                        {"maskMe":"***"}
                        """
        );
    }

    @Test
    void shouldMaskJsonLinesArrays() {
        JsonMasker jsonMasker = JsonMasker.getMasker(Set.of("maskMe"));

        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker,
                """
                        [{"maskMe":"secret"}]
                        [{"maskMe":"secret"}]
                        [{"maskMe":"secret"}]
                        """,
                """
                        [{"maskMe":"***"}]
                        [{"maskMe":"***"}]
                        [{"maskMe":"***"}]
                        """
        );
    }

    @Test
    void shouldMaskJsonLinesMixedTypes() {
        JsonMasker jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder().allowKeys().build());

        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker,
                """
                        "secret"
                        true
                        false
                        null
                        123
                        """,
                """
                        "***"
                        "&&&"
                        "&&&"
                        null
                        "###"
                        """
        );
    }

    @Test
    void shouldMaskRepeatedJsonsWithoutNewLines() {
        JsonMasker jsonMasker = JsonMasker.getMasker(Set.of("maskMe"));

        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker,
                """
                        {"maskMe":"secret"}{"maskMe":"secret"}   {"maskMe":"secret"}
                        """,
                """
                        {"maskMe":"***"}{"maskMe":"***"}   {"maskMe":"***"}
                        """
        );
    }
}
