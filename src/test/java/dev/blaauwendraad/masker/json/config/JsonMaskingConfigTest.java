package dev.blaauwendraad.masker.json.config;

import dev.blaauwendraad.masker.json.path.JsonPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

final class JsonMaskingConfigTest {

    @ParameterizedTest
    @MethodSource("jsonPathsResolved")
    void jsonPathsCorrectlyResolved(
            Set<String> targets,
            Set<String> expectedTargetKeys,
            Set<JsonPath> expectedJsonPaths
    ) {
        Assertions.assertEquals(expectedJsonPaths, JsonMaskingConfig.getDefault(targets).getTargetJsonPaths());
        Assertions.assertEquals(expectedTargetKeys, JsonMaskingConfig.getDefault(targets).getTargetKeys());
    }

    @ParameterizedTest
    @MethodSource("invalidBuilders")
    void invalidBuilders(JsonMaskingConfig.Builder builder) {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new JsonMaskingConfig(builder));
    }

    private static Stream<Arguments> jsonPathsResolved() {
        return Stream.of(
                Arguments.of(
                        Set.of("hello"), Set.of("hello"), Set.of()
                ),
                Arguments.of(
                        Set.of("hello", "$.hello"), Set.of("hello"), Set.of(JsonPath.from("$.hello"))
                ),
                Arguments.of(
                        Set.of("$.hello"), Set.of(), Set.of(JsonPath.from("$.hello"))
                )
        );
    }

    private static Stream<JsonMaskingConfig.Builder> invalidBuilders() {
        return Stream.of(
                JsonMaskingConfig.custom(Set.of(), JsonMaskingConfig.TargetKeyMode.MASK),
                JsonMaskingConfig.custom(
                        Set.of("hello"),
                        JsonMaskingConfig.TargetKeyMode.MASK
                ).maskNumericValuesWith(1).obfuscationLength(0)
        );
    }
}
