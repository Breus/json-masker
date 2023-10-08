package dev.blaauwendraad.masker.json.config;

import dev.blaauwendraad.masker.json.path.JsonPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

class JsonMaskingConfigTest {

    @ParameterizedTest
    @MethodSource("buildersWithAlgorithmType")
    void algorithmTypeSelection(JsonMaskingConfig.Builder builder, JsonMaskerAlgorithmType expectedAlgorithmType) {
        Assertions.assertEquals(expectedAlgorithmType, new JsonMaskingConfig(builder).getAlgorithmType());
    }

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
        Assertions.assertThrows(IllegalArgumentException.class, () -> new JsonMaskingConfig(builder));
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
                JsonMaskingConfig.custom(Set.of()),
                JsonMaskingConfig.custom(Set.of("hello")).maskNumberValuesWith(1).obfuscationLength(0)
        );
    }

    private static Stream<Arguments> buildersWithAlgorithmType() {
        return Stream.of(
                Arguments.of(
                        JsonMaskingConfig.custom(Set.of("oneKey")), JsonMaskerAlgorithmType.SINGLE_TARGET_LOOP
                ),
                Arguments.of(
                        JsonMaskingConfig.custom(Set.of("oneKey", "secondKey")),
                        JsonMaskerAlgorithmType.KEYS_CONTAIN
                ),
                Arguments.of(
                        JsonMaskingConfig.custom(Set.of("$.path", "otherKey")),
                        JsonMaskerAlgorithmType.PATH_AWARE_KEYS_CONTAIN
                ),
                Arguments.of(
                        JsonMaskingConfig.custom(Set.of("$.path", "otherKey"))
                                .algorithmTypeOverride(JsonMaskerAlgorithmType.SINGLE_TARGET_LOOP),
                        JsonMaskerAlgorithmType.SINGLE_TARGET_LOOP
                ),
                Arguments.of(
                        JsonMaskingConfig.custom(Set.of("$.path", "otherKey"))
                                .algorithmTypeOverride(JsonMaskerAlgorithmType.KEYS_CONTAIN),
                        JsonMaskerAlgorithmType.KEYS_CONTAIN
                ),
                Arguments.of(
                        JsonMaskingConfig.custom(Set.of("oneKey", "secondKey"))
                                .algorithmTypeOverride(JsonMaskerAlgorithmType.PATH_AWARE_KEYS_CONTAIN),
                        JsonMaskerAlgorithmType.PATH_AWARE_KEYS_CONTAIN
                )
        );
    }
}
