package dev.blaauwendraad.masker.json;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;


final class PreserveLengthTest {
    @ParameterizedTest
    @MethodSource("lengthObfuscationFile")
    void lengthObfuscation(JsonMaskerTestInstance testInstance) {
        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(testInstance.jsonMasker(),
                testInstance.input(), testInstance.expectedOutput());
    }

    private static Stream<JsonMaskerTestInstance> lengthObfuscationFile() throws IOException {
        return JsonMaskerTestUtil.getJsonMaskerTestInstancesFromFile("test-preserve-length.json").stream();
    }
}
