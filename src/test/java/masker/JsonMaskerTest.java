package masker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

public class JsonMaskerTest {
    static final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("inputOutputMaskAb")
    void testMaskJsonStringValueTargetKey(String input, String expectedOutput) {
        Assertions.assertEquals(expectedOutput, JsonMasker.getDefaultMasker("ab").mask(input));
    }

    @ParameterizedTest
    @MethodSource("inputOutputMaskAb")
    void testMaskJsonStringValueTargetKeyByteArray(String input, String expectedOutput) {
        Assertions.assertArrayEquals(expectedOutput.getBytes(StandardCharsets.UTF_8), JsonMasker.getDefaultMasker("ab").mask(input.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
    }

    @Test
    void testFromJsonFile() throws IOException {
        ArrayNode jsonArray = mapper.readValue(getClass().getClassLoader().getResource("test-input-output.json"), ArrayNode.class);
        for (JsonNode jsonNode : jsonArray) {
            JsonMasker jsonMasker = JsonMasker.getDefaultMasker(mapper.convertValue(jsonNode.get("targetKey"), String.class));
            JsonNode inputJson = jsonNode.get("input");
            String maskedJson = jsonMasker.mask(inputJson.toString());
            Assertions.assertEquals(jsonNode.get("expectedOutput").toString(), maskedJson);
        }
    }

    @Test
    void testLengthObfuscation() throws IOException {
        ArrayNode jsonArray = mapper.readValue(getClass().getClassLoader().getResource("test-obfuscate-length.json"), ArrayNode.class);
        for (JsonNode jsonNode : jsonArray) {
            MaskingConfig maskingConfig = MaskingConfig.custom().obfuscationLength(mapper.convertValue(jsonNode.get("obfuscationLength"), Integer.class)).build();
            JsonMasker jsonMasker = JsonMasker.getMasker(mapper.convertValue(jsonNode.get("targetKey"), String.class), maskingConfig);
            JsonNode inputJson = jsonNode.get("input");
            String maskedJson = jsonMasker.mask(inputJson.toString());
            Assertions.assertEquals(jsonNode.get("expectedOutput").toString(), maskedJson);
        }
    }

    // Returns a stream of argument pairs containing of an unmasked message and the corresponding masked output when the key "ab"  is masked.
    private static Stream<Arguments> inputOutputMaskAb() {
        return Stream.of(
                Arguments.of(objectNode().set("ab", mapper.convertValue("value", JsonNode.class)).toString(), objectNode().set("ab", mapper.convertValue("*****", JsonNode.class)).toString()),
                Arguments.of(objectNode().set("cab", mapper.convertValue("value", JsonNode.class)).toString(), objectNode().set("cab", mapper.convertValue("value", JsonNode.class)).toString()),
                Arguments.of(objectNode().set("ab", mapper.convertValue("", JsonNode.class)).toString(), objectNode().set("ab", mapper.convertValue("", JsonNode.class)).toString()),
                Arguments.of(objectNode().set("cab", objectNode().set("ab", mapper.convertValue("hello", JsonNode.class))).toString(),
                        objectNode().set("cab", objectNode().set("ab", mapper.convertValue("*****", JsonNode.class))).toString()),
                Arguments.of(objectNode().set("ab", objectNode().set("ab", mapper.convertValue("hello", JsonNode.class))).toString(),
                        objectNode().set("ab", objectNode().set("ab", mapper.convertValue("*****", JsonNode.class))).toString()),
                Arguments.of(objectNode().set("ab", objectNode().set("cab", mapper.convertValue("ab", JsonNode.class))).toString(),
                        objectNode().set("ab", objectNode().set("cab", mapper.convertValue("ab", JsonNode.class))).toString()),
                Arguments.of(objectNode().set("ba", objectNode().set("ab", mapper.convertValue("ab", JsonNode.class))).toString(),
                        objectNode().set("ba", objectNode().set("ab", mapper.convertValue("**", JsonNode.class))).toString()),
                Arguments.of(objectNode().set("ab", mapper.convertValue("lo", JsonNode.class)).toString(), objectNode().set("ab", mapper.convertValue("**", JsonNode.class)).toString())
        );
    }

    private static ObjectNode objectNode() {
        return JsonNodeFactory.instance.objectNode();
    }
}