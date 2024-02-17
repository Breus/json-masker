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
                () -> JsonMaskingConfig.builder().maskKeys(Set.of(), KeyMaskingConfig.builder().build()),
                () -> JsonMaskingConfig.builder().maskJsonPaths(Set.of()),
                () -> JsonMaskingConfig.builder().maskJsonPaths(Set.of(), KeyMaskingConfig.builder().build()),
                () -> JsonMaskingConfig.builder().maskKeys(Set.of("maskMe")).maskKeys(Set.of("maskMe")),
                () -> JsonMaskingConfig.builder().maskKeys(Set.of("maskMe")).allowKeys(Set.of("allowMe")),
                () -> JsonMaskingConfig.builder().maskKeys(Set.of("maskMe")).allowJsonPaths(Set.of("$.allowMe")),
                () -> JsonMaskingConfig.builder().allowJsonPaths(Set.of()),
                () -> JsonMaskingConfig.builder().allowKeys(Set.of("allowMe")).allowKeys(Set.of("allowMe")),
                () -> JsonMaskingConfig.builder().allowKeys(Set.of("allowMe")).maskKeys(Set.of("maskMe")),
                () -> JsonMaskingConfig.builder().allowKeys(Set.of("allowMe")).maskJsonPaths(Set.of("$.maskMe")),
                () -> JsonMaskingConfig.builder().allowJsonPaths(Set.of("$.allowMe")).allowJsonPaths(Set.of("$.allowMe")),
                () -> JsonMaskingConfig.builder().allowJsonPaths(Set.of("$.allowMe")).maskKeys(Set.of("maskMe")),
                () -> JsonMaskingConfig.builder().allowJsonPaths(Set.of("$.allowMe")).maskJsonPaths(Set.of("$.maskMe")),
                () -> JsonMaskingConfig.builder().caseSensitiveTargetKeys().caseSensitiveTargetKeys(),
                () -> JsonMaskingConfig.builder().maskStringsWith("***").maskStringsWith("***"),
                () -> JsonMaskingConfig.builder().maskStringsWith("***").maskStringCharactersWith("*"),
                () -> JsonMaskingConfig.builder().maskStringCharactersWith("*").maskStringCharactersWith("*"),
                () -> JsonMaskingConfig.builder().maskNumbersWith("###").maskNumbersWith("###"),
                () -> JsonMaskingConfig.builder().maskNumbersWith("###").maskNumbersWith(123),
                () -> JsonMaskingConfig.builder().maskNumbersWith("###").maskNumberDigitsWith(1),
                () -> JsonMaskingConfig.builder().maskNumbersWith(123).maskNumbersWith(123),
                () -> JsonMaskingConfig.builder().maskNumbersWith(123).maskNumberDigitsWith(1),
                () -> JsonMaskingConfig.builder().maskNumberDigitsWith(1).maskNumberDigitsWith(1),
                () -> JsonMaskingConfig.builder().disableNumberMasking().disableNumberMasking(),
                () -> JsonMaskingConfig.builder().disableNumberMasking().maskNumbersWith("###"),
                () -> JsonMaskingConfig.builder().disableNumberMasking().maskNumbersWith(123),
                () -> JsonMaskingConfig.builder().disableNumberMasking().maskNumberDigitsWith(1),
                () -> JsonMaskingConfig.builder().maskNumberDigitsWith(0),
                () -> JsonMaskingConfig.builder().maskBooleansWith("&&&").maskBooleansWith("&&&"),
                () -> JsonMaskingConfig.builder().maskBooleansWith(false).maskBooleansWith(false),
                () -> JsonMaskingConfig.builder().maskBooleansWith("&&&").maskBooleansWith(false),
                () -> JsonMaskingConfig.builder().disableBooleanMasking().disableBooleanMasking(),
                () -> JsonMaskingConfig.builder().disableBooleanMasking().maskBooleansWith("&&&"),
                () -> JsonMaskingConfig.builder().disableBooleanMasking().maskBooleansWith(false)
        );
    }
}
