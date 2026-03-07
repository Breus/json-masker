package dev.blaauwendraad.masker.json;

import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ColonInKeyOrValueTest {

    @ParameterizedTest
    @MethodSource("testContainsColonFile")
    void containsColon(JsonMaskerTestInstance testInstance) {
        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(
                testInstance.jsonMasker(), testInstance.input(), testInstance.expectedOutput());
    }

    private static Stream<JsonMaskerTestInstance> testContainsColonFile() throws IOException {
        return JsonMaskerTestUtil.getJsonMaskerTestInstancesFromFile("test-contains-colon.json").stream();
    }
}
