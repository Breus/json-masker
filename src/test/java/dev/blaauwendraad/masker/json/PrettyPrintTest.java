package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

class PrettyPrintTest {

    @Test
    void prettyPrintMaskingKeyContains() {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode().put("Test", "Value");
        JsonNode jsonNode = JsonNodeFactory.instance.objectNode().set("Test1", objectNode);
        String prettyString = jsonNode.toPrettyString();
        JsonMasker jsonMasker = new KeyContainsMasker(JsonMaskingConfig.builder().maskKeys("Test").build());
        String mask = jsonMasker.mask(prettyString);
        assertThat(JsonMapper.builder()
                .build()
                .readValue(mask, JsonNode.class)
                .findValue("Test")
                .asString()).isEqualTo("***");
    }
}
