package dev.blaauwendraad.masker.json;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;


final class EscapedCharactersTest {
    @ParameterizedTest
    @MethodSource("escapedCharactersFile")
    void escapedCharacters(JsonMaskerTestInstance testInstance) {
        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(testInstance.jsonMasker(), testInstance.input(),
                testInstance.expectedOutput());
    }

    private static Stream<JsonMaskerTestInstance> escapedCharactersFile() throws IOException {
        return JsonMaskerTestUtil.getJsonMaskerTestInstancesFromFile("test-escaped-characters.json").stream();
    }
}
