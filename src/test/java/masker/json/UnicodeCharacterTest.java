package masker.json;

import masker.json.config.JsonMaskerAlgorithmType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;

class UnicodeCharacterTest {
    @ParameterizedTest
    @MethodSource("unicodeCharacterFile")
    void unicodeCharacter(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(testInstance.expectedOutput(), testInstance.jsonMasker().mask(testInstance.input()));
    }

    private static Stream<JsonMaskerTestInstance> unicodeCharacterFile() throws IOException {
        return JsonMaskerTestUtil.getJsonMaskerTestInstancesFromFile("test-unicode-characters.json", Set.of(
                JsonMaskerAlgorithmType.values())).stream();
    }
}
