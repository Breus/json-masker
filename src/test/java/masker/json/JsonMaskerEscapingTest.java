package masker.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

public class JsonMaskerEscapingTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("testEscapedCharactersFile")
    void testEscapedCharactersSingleTargetLoop(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(testInstance.expectedOutput(), JsonMasker.getMasker(testInstance.targetKeys(), JsonMaskingConfig.custom().multiTargetAlgorithm(JsonMultiTargetAlgorithm.SINGLE_TARGET_LOOP).build()).mask(testInstance.input()));
    }

    @ParameterizedTest
    @MethodSource("testEscapedCharactersFile")
    void testEscapedCharactersKeysContainAlgorithm(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(testInstance.expectedOutput(), JsonMasker.getMasker(testInstance.targetKeys(), JsonMaskingConfig.custom().multiTargetAlgorithm(JsonMultiTargetAlgorithm.KEYS_CONTAIN).build()).mask(testInstance.input()));
    }

    private static Stream<JsonMaskerTestInstance> testEscapedCharactersFile() throws IOException {
        ArrayNode jsonArray = mapper.readValue(JsonMaskerTest.class.getClassLoader().getResource("test-escaped-characters.json"), ArrayNode.class);
        return JsonMaskerTest.getMultipleTargetJsonTestInstanceFromJsonArray(jsonArray).stream();
    }
}
