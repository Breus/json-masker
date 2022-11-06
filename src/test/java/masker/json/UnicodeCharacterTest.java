package masker.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

class UnicodeCharacterTest {
    private static final ObjectMapper mapper = new ObjectMapper();


    @ParameterizedTest
    @MethodSource("unicodeCharactersFile")
    void unicodeCharactersSingleTargetLoopAlgortihm(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(testInstance.expectedOutput(),
                                JsonMasker.getMasker(testInstance.targetKeys(),
                                                     JsonMaskingConfig.custom()
                                                             .multiTargetAlgorithm(JsonMultiTargetAlgorithm.SINGLE_TARGET_LOOP)
                                                             .build()).mask(testInstance.input()));
    }

    @ParameterizedTest
    @MethodSource("unicodeCharactersFile")
    void unicodeCharactersKeyContainsAlgorithm(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(testInstance.expectedOutput(),
                                JsonMasker.getMasker(testInstance.targetKeys(),
                                                     JsonMaskingConfig.custom()
                                                             .multiTargetAlgorithm(JsonMultiTargetAlgorithm.KEYS_CONTAIN)
                                                             .build()).mask(testInstance.input()));
    }

    @Test
    void unicodeCharacter() {
        String input = "{\"someKey\": \"\u2020\"}";
        String output = "{\"someKey\": \"*\"}";
        Assertions.assertEquals(output,
                                JsonMasker.getMasker("someKey",
                                                     JsonMaskingConfig.custom()
                                                             .multiTargetAlgorithm(JsonMultiTargetAlgorithm.KEYS_CONTAIN)
                                                             .build()).mask(input));
    }

    private static Stream<JsonMaskerTestInstance> unicodeCharactersFile() throws IOException {
        ArrayNode jsonArray =
                mapper.readValue(JsonMaskerTest.class.getClassLoader().getResource("test-unicode-characters.json"),
                                 ArrayNode.class);
        return getJsonTestInstancesFromJsonArray(jsonArray).stream();
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
}
