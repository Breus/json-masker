package masker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ReplaceTest {
    final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testMaskJsonStringValueFilterKey() {
        String input = objectNode().set("ab", mapper.convertValue("value", JsonNode.class)).toString();
        String maskedInput = "{\"ab\":\"*****\"}";
        String filterKey = "ab";
        Assertions.assertEquals(maskedInput, Masker.maskValueOfKeyJson(input, filterKey));
    }

    @Test
    void testMaskJsonStringValueFilterKeyNotInObject() {
        String input = objectNode().set("cab", mapper.convertValue("value", JsonNode.class)).toString();
        String expectedOutput = "{\"cab\":\"value\"}";
        String filterKey = "ab";
        Assertions.assertEquals(expectedOutput, Masker.maskValueOfKeyJson(input, filterKey));
    }

    ObjectNode objectNode() {
        return JsonNodeFactory.instance.objectNode();
    }
}
