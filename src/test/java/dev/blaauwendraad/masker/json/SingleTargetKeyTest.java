package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskerAlgorithmType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;

final class SingleTargetKeyTest {
    @ParameterizedTest
    @MethodSource("singleTargetKeyFile")
    void singleTargetKey(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(testInstance.expectedOutput(), testInstance.jsonMasker().mask(testInstance.input()));
    }

    private static Stream<JsonMaskerTestInstance> singleTargetKeyFile() throws IOException {
        return JsonMaskerTestUtil.getJsonMaskerTestInstancesFromFile("test-single-target-key.json", Set.of(
                JsonMaskerAlgorithmType.values())).stream();
    }
}
