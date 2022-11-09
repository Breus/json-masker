package masker.json;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class ColonInKeyOrValueTest {
    @Test
    void objectContainingColon() {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode().put("targetKey:1", ":val:ue\\::");
        Assertions.assertDoesNotThrow(() -> new SingleTargetMasker(JsonMaskingConfig.getDefault(Set.of("targetKey:1"))).mask(objectNode.toString()));
        Assertions.assertDoesNotThrow(() -> new KeyContainsMasker(JsonMaskingConfig.getDefault(Set.of("targetKey:1"))).mask(objectNode.toString()));
        Assertions.assertDoesNotThrow(() -> new PathAwareKeyContainsMasker(JsonMaskingConfig.getDefault(Set.of("targetKey:1"))).mask(objectNode.toString()));
    }

    @Test
    void stringContainingColon() {
        TextNode textNode = TextNode.valueOf("thisIsValidJson:");
        Assertions.assertDoesNotThrow(() -> new SingleTargetMasker(JsonMaskingConfig.getDefault(Set.of(""))).mask(textNode.asText()));
        Assertions.assertDoesNotThrow(() -> new KeyContainsMasker(JsonMaskingConfig.getDefault(Set.of(""))).mask(textNode.asText()));
        Assertions.assertDoesNotThrow(() -> new PathAwareKeyContainsMasker(JsonMaskingConfig.getDefault(Set.of(""))).mask(textNode.asText()));
    }
}
