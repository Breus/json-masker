package masker.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

class NumberMaskingTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("testNumberMaskingFile")
    void numberMaskingSingleTargetLoop(JsonMaskerTestInstance testInstance) {
        JsonMaskingConfig maskingConfig = JsonMaskingConfig.custom(testInstance.targetKeys())
                .obfuscationLength(testInstance.obfuscationLength())
                .maskNumberValuesWith(testInstance.maskNumbersWithValue())
                .build();
        Assertions.assertEquals(
                testInstance.expectedOutput(),
                new SingleTargetMasker(maskingConfig).mask(testInstance.input())
        );
    }

    @ParameterizedTest
    @MethodSource("testNumberMaskingFile")
    void numberMaskingKeyContains(JsonMaskerTestInstance testInstance) {
        JsonMaskingConfig maskingConfig = JsonMaskingConfig.custom(testInstance.targetKeys())
                .obfuscationLength(testInstance.obfuscationLength())
                .maskNumberValuesWith(testInstance.maskNumbersWithValue())
                .build();
        Assertions.assertEquals(
                testInstance.expectedOutput(),
                new KeyContainsMasker(maskingConfig).mask(testInstance.input())
        );
    }

    @ParameterizedTest
    @MethodSource("testNumberMaskingFile")
    void numberMaskingPathAwareKeyContains(JsonMaskerTestInstance testInstance) {
        JsonMaskingConfig maskingConfig = JsonMaskingConfig.custom(testInstance.targetKeys())
                .obfuscationLength(testInstance.obfuscationLength())
                .maskNumberValuesWith(testInstance.maskNumbersWithValue())
                .build();
        Assertions.assertEquals(
                testInstance.expectedOutput(),
                new PathAwareKeyContainsMasker(maskingConfig).mask(testInstance.input())
        );
    }


    private static Stream<JsonMaskerTestInstance> testNumberMaskingFile() throws IOException {
        ArrayNode jsonArray =
                mapper.readValue(JsonMaskerTest.class.getClassLoader().getResource("test-number-values.json"),
                                 ArrayNode.class);
        return JsonMaskerTest.getMultipleTargetJsonTestInstanceFromJsonArray(jsonArray).stream();
    }
}
