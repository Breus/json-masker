package masker.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

@Disabled("Number masking is work in progress, but test code is already here for TDD")
class JsonNumberMaskingTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("testNumberMaskingFile")
    void testNumberMasking(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(testInstance.expectedOutput(), JsonMasker.getMasker(testInstance.targetKeys(), JsonMaskingConfig.custom().obfuscationLength(testInstance.obfuscationLength()).maskNumberValuesWith(1).build()).mask(testInstance.input()));
    }

    private static Stream<JsonMaskerTestInstance> testNumberMaskingFile() throws IOException {
        ArrayNode jsonArray = mapper.readValue(JsonMaskerTest.class.getClassLoader().getResource("test-number-values.json"), ArrayNode.class);
        return JsonMaskerTest.getMultipleTargetJsonTestInstanceFromJsonArray(jsonArray).stream();
    }
}
