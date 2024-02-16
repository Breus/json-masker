package dev.blaauwendraad.masker.json.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

final class JsonMaskingConfigTest {

    @ParameterizedTest
    @MethodSource("invalidBuilders")
    void invalidBuilders(Supplier<JsonMaskingConfig.Builder> builder) {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> builder.get().build());
    }

    private static Stream<Supplier<JsonMaskingConfig.Builder>> invalidBuilders() {
        return Stream.of(
                () -> JsonMaskingConfig.builder(),
                () -> JsonMaskingConfig.builder().maskKeys(Set.of()),
                () -> JsonMaskingConfig.builder().maskJsonPaths(Set.of()),
                () -> JsonMaskingConfig.builder().maskKeys(Set.of("maskMe")).maskKeys(Set.of("maskMe")),
                () -> JsonMaskingConfig.builder().maskKeys(Set.of("maskMe")).allowKeys(Set.of("allowMe")),
                () -> JsonMaskingConfig.builder().allowJsonPaths(Set.of()),
                () -> JsonMaskingConfig.builder().allowKeys(Set.of("allowMe")).allowKeys(Set.of("allowMe")),
                () -> JsonMaskingConfig.builder().allowKeys(Set.of("allowMe")).maskKeys(Set.of("maskMe")),
                () -> JsonMaskingConfig.builder().maskKeys(Set.of("maskMe")).caseSensitiveTargetKeys().caseSensitiveTargetKeys()
        );
    }
}
