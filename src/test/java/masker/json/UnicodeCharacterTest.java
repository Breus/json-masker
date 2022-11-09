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
    void unicodeCharacter(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(testInstance.expectedOutput(), new SingleTargetMasker(JsonMaskingConfig.getDefault(testInstance.targetKeys())).mask(testInstance.input()));
        Assertions.assertEquals(testInstance.expectedOutput(), new KeyContainsMasker(JsonMaskingConfig.getDefault(testInstance.targetKeys())).mask(testInstance.input()));
        Assertions.assertEquals(testInstance.expectedOutput(), new PathAwareKeyContainsMasker(JsonMaskingConfig.getDefault(testInstance.targetKeys())).mask(testInstance.input()));
    }

    @Test
    void unicodeCharacter() {
        String input = "{\"someKey\": \"\u2020\"}";
        String output = "{\"someKey\": \"*\"}";
        Assertions.assertEquals(output, new SingleTargetMasker(JsonMaskingConfig.getDefault(Set.of("someKey"))).mask(input));
        Assertions.assertEquals(output, new KeyContainsMasker(JsonMaskingConfig.getDefault(Set.of("someKey"))).mask(input));
        Assertions.assertEquals(output, new PathAwareKeyContainsMasker(JsonMaskingConfig.getDefault(Set.of("someKey"))).mask(input));
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
