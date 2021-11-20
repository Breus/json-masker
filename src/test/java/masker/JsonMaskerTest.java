package masker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class JsonMaskerTest {
    static final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("inputOutputMaskAb")
    void testMaskJsonStringValueFilterKey(String input, String expectedOutput) {
        Assertions.assertEquals(expectedOutput, JsonMasker.getMaskerWithTargetKey("ab").mask(input));
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
                Arguments.of(objectNode().set("ab", mapper.convertValue("lo", JsonNode.class)).toString(), objectNode().set("ab", mapper.convertValue("**", JsonNode.class)).toString())
        );
    }

    private static ObjectNode objectNode() {
        return JsonNodeFactory.instance.objectNode();
    }
}
