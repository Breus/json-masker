package masker.json;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ColonInkeyOrValueTest {
    @Test
    void objectContainingColon() {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode().put("targetKey:1", ":val:ue\\::");
        Assertions.assertDoesNotThrow(() -> JsonMasker.getMasker("targetKey:1", JsonMaskingConfig.custom().multiTargetAlgorithm(JsonMultiTargetAlgorithm.KEYS_CONTAIN).build()).mask(objectNode.toString()));
        Assertions.assertDoesNotThrow(() -> JsonMasker.getMasker("targetKey:1", JsonMaskingConfig.custom().multiTargetAlgorithm(JsonMultiTargetAlgorithm.SINGLE_TARGET_LOOP).build()).mask(objectNode.toString()));
    }


    @Test
    void stringContainingColon() {
        TextNode textNode = TextNode.valueOf("thisIsValidJson:");
        Assertions.assertDoesNotThrow(() -> JsonMasker.getMasker("", JsonMaskingConfig.custom().multiTargetAlgorithm(JsonMultiTargetAlgorithm.SINGLE_TARGET_LOOP).build()).mask(textNode.asText()));
        Assertions.assertDoesNotThrow(() -> JsonMasker.getMasker("", JsonMaskingConfig.custom().multiTargetAlgorithm(JsonMultiTargetAlgorithm.KEYS_CONTAIN).build()).mask(textNode.asText()));
    }
}