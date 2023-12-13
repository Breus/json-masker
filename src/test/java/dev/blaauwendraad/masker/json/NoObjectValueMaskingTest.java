package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskerAlgorithmType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
final class NoObjectValueMaskingTest {
    @ParameterizedTest
    @MethodSource("noObjectMaskingFile")
    void multiTargetKey(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(testInstance.expectedOutput(), testInstance.jsonMasker().mask(testInstance.input()));
    }

    private static Stream<JsonMaskerTestInstance> noObjectMaskingFile() throws IOException {
        return JsonMaskerTestUtil.getJsonMaskerTestInstancesFromFile("test-no-object-value-masking.json", Set.of(
                JsonMaskerAlgorithmType.values())).stream();
    }

}
