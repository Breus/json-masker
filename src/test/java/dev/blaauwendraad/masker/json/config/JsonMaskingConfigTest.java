package dev.blaauwendraad.masker.json.config;

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
    @MethodSource("invalidBuilders")
    void invalidBuilders(JsonMaskingConfig.Builder builder) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new JsonMaskingConfig(builder));
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

    private static Stream<Arguments> buildersWithAlgorithmType() {
        return Stream.of(
                Arguments.of(
                        JsonMaskingConfig.custom(Set.of("oneKey", "secondKey"), JsonMaskingConfig.TargetKeyMode.MASK),
                        JsonMaskerAlgorithmType.KEYS_CONTAIN
                ),
                Arguments.of(
                        JsonMaskingConfig.custom(Set.of("$.path", "otherKey"), JsonMaskingConfig.TargetKeyMode.MASK)
                                .algorithmTypeOverride(JsonMaskerAlgorithmType.KEYS_CONTAIN),
                        JsonMaskerAlgorithmType.KEYS_CONTAIN
                )
        );
    }
}
