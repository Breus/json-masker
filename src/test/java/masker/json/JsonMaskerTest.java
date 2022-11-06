package masker.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

class JsonMaskerTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("inputOutputMaskAb")
    void maskJsonStringValueTargetKey(String input, String expectedOutput) {
        Assertions.assertEquals(expectedOutput, JsonMasker.getMasker("ab").mask(input));
    }

    @ParameterizedTest
    @MethodSource("inputOutputMaskAb")
    void maskJsonStringValueTargetKeyByteArray(String input, String expectedOutput) {
        Assertions.assertArrayEquals(expectedOutput.getBytes(StandardCharsets.UTF_8),
                                     JsonMasker.getMasker("ab")
                                             .mask(input.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @MethodSource("testSingleTargetKeyFile")
    void singleTargetSingleTargetLoopAlgorithm(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(testInstance.expectedOutput(),
                                JsonMasker.getMasker(testInstance.targetKeys(),
                                                     JsonMaskingConfig.custom()
                                                             .multiTargetAlgorithm(JsonMultiTargetAlgorithm.SINGLE_TARGET_LOOP)
                                                             .build()).mask(testInstance.input()));
    }

    @ParameterizedTest
    @MethodSource("testSingleTargetKeyFile")
    void singleTargetKeyKeyContainsAlgorithm(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(testInstance.expectedOutput(),
                                JsonMasker.getMasker(testInstance.targetKeys(),
                                                     JsonMaskingConfig.custom()
                                                             .multiTargetAlgorithm(JsonMultiTargetAlgorithm.KEYS_CONTAIN)
                                                             .build()).mask(testInstance.input()));
    }

    @ParameterizedTest
    @MethodSource("testMultipleTargetKeyFile")
    void multipleTargetsSingleTargetLoopAlgorithm(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(testInstance.expectedOutput(),
                                JsonMasker.getMasker(testInstance.targetKeys(),
                                                     JsonMaskingConfig.custom()
                                                             .multiTargetAlgorithm(JsonMultiTargetAlgorithm.SINGLE_TARGET_LOOP)
                                                             .build()).mask(testInstance.input()));
    }

    @ParameterizedTest
    @MethodSource("testMultipleTargetKeyFile")
    void multipleTargetskeysContainAlgorithm(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(testInstance.expectedOutput(),
                                JsonMasker.getMasker(testInstance.targetKeys(),
                                                     JsonMaskingConfig.custom()
                                                             .multiTargetAlgorithm(JsonMultiTargetAlgorithm.KEYS_CONTAIN)
                                                             .build()).mask(testInstance.input()));
    }


    @ParameterizedTest
    @MethodSource("testObfuscationFile")
    void lengthObfuscationFromFileSingleTargetLoopAlgorithm(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(testInstance.expectedOutput(),
                                JsonMasker.getMasker(testInstance.targetKeys(),
                                                     JsonMaskingConfig.custom()
                                                             .multiTargetAlgorithm(JsonMultiTargetAlgorithm.SINGLE_TARGET_LOOP)
                                                             .obfuscationLength(testInstance.obfuscationLength())
                                                             .build()).mask(testInstance.input()));
    }

    @ParameterizedTest
    @MethodSource("testObfuscationFile")
    void lengthObfuscationFromFileKeyContainsAlgorithm(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(testInstance.expectedOutput(),
                                JsonMasker.getMasker(testInstance.targetKeys(),
                                                     JsonMaskingConfig.custom()
                                                             .multiTargetAlgorithm(JsonMultiTargetAlgorithm.KEYS_CONTAIN)
                                                             .obfuscationLength(testInstance.obfuscationLength())
                                                             .build()).mask(testInstance.input()));
    }

    // Returns a stream of argument pairs containing of an unmasked message and the corresponding masked output when the key "ab"  is masked.
    private static Stream<Arguments> inputOutputMaskAb() {
        return Stream.of(Arguments.of(objectNode().set("ab", mapper.convertValue("value", JsonNode.class)).toString(),
                                      objectNode().set("ab", mapper.convertValue("*****", JsonNode.class)).toString()),
                         Arguments.of(objectNode().set("cab", mapper.convertValue("value", JsonNode.class)).toString(),
                                      objectNode().set("cab", mapper.convertValue("value", JsonNode.class)).toString()),
                         Arguments.of(objectNode().set("ab", mapper.convertValue("", JsonNode.class)).toString(),
                                      objectNode().set("ab", mapper.convertValue("", JsonNode.class)).toString()),
                         Arguments.of(objectNode().set("cab",
                                                       objectNode().set("ab",
                                                                        mapper.convertValue("hello", JsonNode.class)))
                                              .toString(),
                                      objectNode().set("cab",
                                                       objectNode().set("ab",
                                                                        mapper.convertValue("*****", JsonNode.class)))
                                              .toString()),
                         Arguments.of(objectNode().set("ab",
                                                       objectNode().set("ab",
                                                                        mapper.convertValue("hello", JsonNode.class)))
                                              .toString(),
                                      objectNode().set("ab",
                                                       objectNode().set("ab",
                                                                        mapper.convertValue("*****", JsonNode.class)))
                                              .toString()),
                         Arguments.of(objectNode().set("ab",
                                                       objectNode().set("cab",
                                                                        mapper.convertValue("ab", JsonNode.class)))
                                              .toString(),
                                      objectNode().set("ab",
                                                       objectNode().set("cab",
                                                                        mapper.convertValue("ab", JsonNode.class)))
                                              .toString()),
                         Arguments.of(objectNode().set("ba",
                                                       objectNode().set("ab",
                                                                        mapper.convertValue("ab", JsonNode.class)))
                                              .toString(),
                                      objectNode().set("ba",
                                                       objectNode().set("ab",
                                                                        mapper.convertValue("**", JsonNode.class)))
                                              .toString()),
                         Arguments.of(objectNode().set("ab", mapper.convertValue("lo", JsonNode.class)).toString(),
                                      objectNode().set("ab", mapper.convertValue("**", JsonNode.class)).toString()));
    }

    @Test
    void malformedJsonMaskingSingleTargetLoopAlgorithm() throws IOException, URISyntaxException {
        final String TARGET_KEY = "targetKey";
        URL resourceUrl = JsonMaskerTest.class.getClassLoader().getResource("malformed-json.txt");
        Assertions.assertNotNull(resourceUrl);
        Path fileName = Path.of(resourceUrl.toURI());
        Assertions.assertNotNull(fileName);
        String malformedJsonMessage = Files.readString(fileName);
        JsonNode jsonNode = mapper.readTree(malformedJsonMessage);
        String targetKeyValue = mapper.convertValue(jsonNode.findValue(TARGET_KEY), String.class);
        JsonMaskingConfig jsonMaskingConfig =
                JsonMaskingConfig.custom().multiTargetAlgorithm(JsonMultiTargetAlgorithm.SINGLE_TARGET_LOOP).build();
        String maskedJsonMessage = JsonMasker.getMasker(TARGET_KEY, jsonMaskingConfig).mask(malformedJsonMessage);
        JsonNode maskedJsonNode = mapper.readTree(maskedJsonMessage);
        String maskedKeyValue = "*".repeat(targetKeyValue.length());
        Assertions.assertEquals(maskedKeyValue,
                                mapper.convertValue(maskedJsonNode.findValue(TARGET_KEY), String.class));
    }

    @Test
    void malformedJsonMaskingKeyContainsAlgorithm() throws IOException, URISyntaxException {
        final String TARGET_KEY = "targetKey";
        URL resourceUrl = JsonMaskerTest.class.getClassLoader().getResource("malformed-json.txt");
        Assertions.assertNotNull(resourceUrl);
        Path fileName = Path.of(resourceUrl.toURI());
        Assertions.assertNotNull(fileName);
        String malformedJsonMessage = Files.readString(fileName);
        JsonNode jsonNode = mapper.readTree(malformedJsonMessage);
        String targetKeyValue = mapper.convertValue(jsonNode.findValue(TARGET_KEY), String.class);
        JsonMaskingConfig jsonMaskingConfig =
                JsonMaskingConfig.custom().multiTargetAlgorithm(JsonMultiTargetAlgorithm.KEYS_CONTAIN).build();
        String maskedJsonMessage = JsonMasker.getMasker(TARGET_KEY, jsonMaskingConfig).mask(malformedJsonMessage);
        JsonNode maskedJsonNode = mapper.readTree(maskedJsonMessage);
        String maskedKeyValue = "*".repeat(targetKeyValue.length());
        Assertions.assertEquals(maskedKeyValue,
                                mapper.convertValue(maskedJsonNode.findValue(TARGET_KEY), String.class));
    }

    private static Stream<JsonMaskerTestInstance> testSingleTargetKeyFile() throws IOException {
        ArrayNode jsonArray =
                mapper.readValue(JsonMaskerTest.class.getClassLoader().getResource("test-single-target-key.json"),
                                 ArrayNode.class);
        return getJsonTestInstancesFromJsonArray(jsonArray).stream();
    }

    private static Stream<JsonMaskerTestInstance> testMultipleTargetKeyFile() throws IOException {
        ArrayNode jsonArray =
                mapper.readValue(JsonMaskerTest.class.getClassLoader().getResource("test-multiple-target-keys.json"),
                                 ArrayNode.class);
        return getMultipleTargetJsonTestInstanceFromJsonArray(jsonArray).stream();
    }

    private static Stream<JsonMaskerTestInstance> testObfuscationFile() throws IOException {
        ArrayNode jsonArray =
                mapper.readValue(JsonMaskerTest.class.getClassLoader().getResource("test-obfuscate-length.json"),
                                 ArrayNode.class);
        return getJsonTestInstancesFromJsonArray(jsonArray).stream();
    }

    static List<JsonMaskerTestInstance> getMultipleTargetJsonTestInstanceFromJsonArray(ArrayNode jsonArray) throws IOException {
        ArrayList<JsonMaskerTestInstance> testInstances = new ArrayList<>();
        ObjectReader reader =
                mapper.readerFor(TypeFactory.defaultInstance().constructCollectionType(Set.class, String.class));
        for (JsonNode jsonNode : jsonArray) {
            Map<String, Object> maskerConfigs = null;
            if (jsonNode.findValue("maskerConfig") != null) {
                maskerConfigs = reader.readValue(jsonNode.get("maskerConfig"), Map.class);
            }
            var jsonMaskerTestInstance = new JsonMaskerTestInstance(reader.readValue(jsonNode.get("targetKeys")),
                                                                    jsonNode.get("input").toString(),
                                                                    jsonNode.get("expectedOutput").toString(),
                                                                    maskerConfigs);
            testInstances.add(jsonMaskerTestInstance);
        }
        return testInstances;
    }

    private static List<JsonMaskerTestInstance> getJsonTestInstancesFromJsonArray(ArrayNode jsonArray) throws IOException {
        ArrayList<JsonMaskerTestInstance> testInstances = new ArrayList<>();
        ObjectReader reader = mapper.readerFor(new TypeReference<Set<String>>() {});
        for (JsonNode jsonNode : jsonArray) {
            Map<String, Object> maskerConfigs = null;
            if (jsonNode.findValue("maskerConfig") != null) {
                maskerConfigs = reader.readValue(jsonNode.get("maskerConfig"), Map.class);
            }
            var jsonMaskerTestInstance =
                    new JsonMaskerTestInstance(Set.of(mapper.convertValue(jsonNode.get("targetKey"), String.class)),
                                               jsonNode.get("input").toString(),
                                               jsonNode.get("expectedOutput").toString(),
                                               maskerConfigs);
            testInstances.add(jsonMaskerTestInstance);
        }
        return testInstances;
    }

    private static ObjectNode objectNode() {
        return JsonNodeFactory.instance.objectNode();
    }
}