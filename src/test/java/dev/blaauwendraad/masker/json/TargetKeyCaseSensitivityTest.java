package dev.blaauwendraad.masker.json;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;


final class TargetKeyCaseSensitivityTest {
    @ParameterizedTest
    @MethodSource("caseSensitivityFile")
    void caseSensitivityInTargetKeys(JsonMaskerTestInstance testInstance) {
        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(testInstance.jsonMasker(), testInstance.input(),
                testInstance.expectedOutput());
    }

    private static Stream<JsonMaskerTestInstance> caseSensitivityFile() throws IOException {
        return JsonMaskerTestUtil.getJsonMaskerTestInstancesFromFile("test-case-sensitivity.json").stream();
    }
}

