package masker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ReplaceTest {

    @Test
    void testMaskJsonStringValueFilterKey() {
        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonNode = JsonNodeFactory.instance.objectNode();
        jsonNode.set("ab", mapper.convertValue("value", JsonNode.class));
        String input = jsonNode.toString();
        String maskedInput = "{\"ab\":\"*****\"}";
        String filterKey = "ab";
        Assertions.assertEquals(maskedInput, Masker.maskValueOfKeyJson(input, filterKey));
    }

    @Test
    void testMaskJsonStringValueFilterKeyNotInObject() {
        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonNode = JsonNodeFactory.instance.objectNode();
        jsonNode.set("cab", mapper.convertValue("value", JsonNode.class));
        String input = jsonNode.toString();
        String expectedOutput = "{\"cab\":\"value\"}";
        String filterKey = "ab";
        Assertions.assertEquals(expectedOutput, Masker.maskValueOfKeyJson(input, filterKey));
    }
}
