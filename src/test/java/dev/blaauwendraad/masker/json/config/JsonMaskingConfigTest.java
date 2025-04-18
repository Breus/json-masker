package dev.blaauwendraad.masker.json.config;

import dev.blaauwendraad.masker.json.ValueMaskers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
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
                () -> JsonMaskingConfig.builder().maskKeys(Set.of(), KeyMaskingConfig.builder().build()),
                () -> JsonMaskingConfig.builder().maskKeys(Map.of()),
                () -> JsonMaskingConfig.builder().maskKeys("maskMe").maskKeys("maskMe"),
                () -> JsonMaskingConfig.builder().maskKeys("maskMe").allowKeys("allowMe"),
                () -> JsonMaskingConfig.builder().maskKeys("maskMe").allowJsonPaths("$.allowMe"),
                () -> JsonMaskingConfig.builder().maskJsonPaths(Set.of()),
                () -> JsonMaskingConfig.builder().maskJsonPaths(Set.of(), KeyMaskingConfig.builder().build()),
                () -> JsonMaskingConfig.builder().maskJsonPaths(Map.of()),
                () -> JsonMaskingConfig.builder().maskJsonPaths(Map.of()),
                () -> JsonMaskingConfig.builder().maskJsonPaths("$.maskMe").maskJsonPaths("$.maskMe"),
                () -> JsonMaskingConfig.builder().allowKeys("allowMe").allowKeys("allowMe"),
                () -> JsonMaskingConfig.builder().allowKeys("allowMe").maskKeys("maskMe"),
                () -> JsonMaskingConfig.builder().allowKeys("allowMe").maskJsonPaths("$.maskMe"),
                () -> JsonMaskingConfig.builder().allowJsonPaths("$.allowMe").allowJsonPaths("$.allowMe"),
                () -> JsonMaskingConfig.builder().allowJsonPaths("$.allowMe").allowJsonPaths(Set.of("$.allowMe")),
                () -> JsonMaskingConfig.builder().allowJsonPaths("$.allowMe").maskKeys("maskMe"),
                () -> JsonMaskingConfig.builder().allowJsonPaths("$.allowMe").maskJsonPaths("$.maskMe"),
                () -> JsonMaskingConfig.builder().allowJsonPaths("$"),
                () -> JsonMaskingConfig.builder().caseSensitiveTargetKeys().caseSensitiveTargetKeys(),
                () -> JsonMaskingConfig.builder().maskStringsWith("***").maskStringsWith("***"),
                () -> JsonMaskingConfig.builder().maskStringsWith("***").maskStringCharactersWith("*"),
                () -> JsonMaskingConfig.builder().maskStringsWith(ValueMaskers.with("***")).maskStringsWith(ValueMaskers.with("***")),
                () -> JsonMaskingConfig.builder().maskStringCharactersWith("*").maskStringCharactersWith("*"),
                () -> JsonMaskingConfig.builder().maskStringCharactersWith("*").maskStringsWith(ValueMaskers.with("***")),
                () -> JsonMaskingConfig.builder().maskNumbersWith("###").maskNumbersWith("###"),
                () -> JsonMaskingConfig.builder().maskNumbersWith("###").maskNumbersWith(123),
                () -> JsonMaskingConfig.builder().maskNumbersWith("###").maskNumberDigitsWith(1),
                () -> JsonMaskingConfig.builder().maskNumbersWith(123).maskNumbersWith(123),
                () -> JsonMaskingConfig.builder().maskNumbersWith(123).maskNumberDigitsWith(1),
                () -> JsonMaskingConfig.builder().maskNumbersWith(ValueMaskers.with(0)).maskNumbersWith(ValueMaskers.with(0)),
                () -> JsonMaskingConfig.builder().maskNumbersWith(ValueMaskers.with(0)).maskNumbersWith("###"),
                () -> JsonMaskingConfig.builder().maskNumbersWith(ValueMaskers.with(0)).maskNumbersWith(123),
                () -> JsonMaskingConfig.builder().maskNumbersWith(ValueMaskers.with(0)).maskNumberDigitsWith(1),
                () -> JsonMaskingConfig.builder().maskNumberDigitsWith(1).maskNumberDigitsWith(1),
                () -> JsonMaskingConfig.builder().maskNumberDigitsWith(0),
                () -> JsonMaskingConfig.builder().maskNumberDigitsWith(10),
                () -> JsonMaskingConfig.builder().maskBooleansWith("&&&").maskBooleansWith("&&&"),
                () -> JsonMaskingConfig.builder().maskBooleansWith(false).maskBooleansWith(false),
                () -> JsonMaskingConfig.builder().maskBooleansWith("&&&").maskBooleansWith(false),
                () -> JsonMaskingConfig.builder().maskBooleansWith(ValueMaskers.with(false)).maskBooleansWith("&&&"),
                () -> JsonMaskingConfig.builder().maskBooleansWith(ValueMaskers.with(false)).maskBooleansWith(false),
                () -> JsonMaskingConfig.builder().maskBooleansWith(ValueMaskers.with(false)).maskBooleansWith(ValueMaskers.with(false))
        );
    }
}
