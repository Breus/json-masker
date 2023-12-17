package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class PrettyPrintTest {

    @Test
    void prettyPrintMaskingKeyContains() throws JsonProcessingException {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode().put("Test", "Value");
        JsonNode jsonNode = JsonNodeFactory.instance.objectNode().set("Test1", objectNode);
        String prettyString = jsonNode.toPrettyString();
        JsonMasker jsonMasker = new KeyContainsMasker(JsonMaskingConfig.getDefault(Set.of("Test")));
        String mask = jsonMasker.mask(prettyString);
        Assertions.assertEquals(
                "*****",
                JsonMapper.builder()
                        .build()
                        .readValue(mask, JsonNode.class)
                        .findValue("Test")
                        .textValue()
        );
    }
}