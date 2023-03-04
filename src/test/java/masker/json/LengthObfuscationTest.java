package masker.json;

import masker.json.config.JsonMaskerAlgorithmType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;

public class LengthObfuscationTest {
    @ParameterizedTest
    @MethodSource("lengthObfuscationFile")
    void lengthObfuscation(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(testInstance.expectedOutput(), testInstance.jsonMasker().mask(testInstance.input()));
    }

    private static Stream<JsonMaskerTestInstance> lengthObfuscationFile() throws IOException {
        return JsonMaskerTestUtil.getJsonMaskerTestInstancesFromFile("test-obfuscate-length.json", Set.of(
                JsonMaskerAlgorithmType.values())).stream();
    }
}
