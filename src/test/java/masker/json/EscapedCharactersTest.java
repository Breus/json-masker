package masker.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

class EscapedCharactersTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("testEscapedCharactersFile")
    void escapedCharactersSingleTargetLoop(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(
                testInstance.expectedOutput(),
                new SingleTargetMasker(JsonMaskingConfig.getDefault(testInstance.targetKeys())).mask(testInstance.input())
        );
    }

    @ParameterizedTest
    @MethodSource("testEscapedCharactersFile")
    void escapedCharactersKeysContainAlgorithm(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(
                testInstance.expectedOutput(),
                new KeyContainsMasker(JsonMaskingConfig.getDefault(testInstance.targetKeys())).mask(testInstance.input())
        );
    }

    @ParameterizedTest
    @MethodSource("testEscapedCharactersFile")
    void escapedCharactersPathAwareKeyContainsAlgorithm(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(
                testInstance.expectedOutput(),
                new PathAwareKeyContainsMasker(JsonMaskingConfig.getDefault(testInstance.targetKeys())).mask(testInstance.input())
        );
    }

    private static Stream<JsonMaskerTestInstance> testEscapedCharactersFile() throws IOException {
        ArrayNode jsonArray =
                mapper.readValue(JsonMaskerTest.class.getClassLoader().getResource("test-escaped-characters.json"),
                                 ArrayNode.class);
        return JsonMaskerTest.getMultipleTargetJsonTestInstanceFromJsonArray(jsonArray).stream();
    }
}
