package masker.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JsonPrettyPrintTest {
    @Test
    void testPrettyPrintMaskingDefault() throws JsonProcessingException {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode().put("Test", "Value");
        JsonNode jsonNode = JsonNodeFactory.instance.objectNode().set("Test1", objectNode);
        String prettyString = jsonNode.toPrettyString();
        JsonMasker jsonMasker = JsonMasker.getMasker("Test");
        String mask = jsonMasker.mask(prettyString);
        Assertions.assertEquals("*****", JsonMapper.builder().build().readValue(mask, JsonNode.class).findValue("Test").textValue());
    }

    @Test
    void testPrettyPrintMaskingKeyContains() throws JsonProcessingException {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode().put("Test", "Value");
        JsonNode jsonNode = JsonNodeFactory.instance.objectNode().set("Test1", objectNode);
        String prettyString = jsonNode.toPrettyString();
        JsonMasker jsonMasker = JsonMasker.getMasker("Test", JsonMaskingConfig.custom().multiTargetAlgorithm(JsonMultiTargetAlgorithm.KEYS_CONTAIN).build());
        String mask = jsonMasker.mask(prettyString);
        Assertions.assertEquals("*****", JsonMapper.builder().build().readValue(mask, JsonNode.class).findValue("Test").textValue());
    }
}
