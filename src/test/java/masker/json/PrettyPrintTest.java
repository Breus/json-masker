package masker.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class PrettyPrintTest {
    @Test
    void prettyPrintMasking() {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode().put("Test", "Value");
        JsonNode jsonNode = JsonNodeFactory.instance.objectNode().set("Test1", objectNode);
        String prettyString = jsonNode.toPrettyString();
        Assertions.assertAll(
                () -> Assertions.assertEquals(
                        "*****",
                        new SingleTargetMasker(JsonMaskingConfig.getDefault(Set.of("someKey"))).mask(prettyString)
                ),
                () -> Assertions.assertEquals(
                        "*****",
                        new KeyContainsMasker(JsonMaskingConfig.getDefault(Set.of("someKey"))).mask(prettyString)
                ),
                () -> Assertions.assertEquals(
                        "*****",
                        new PathAwareKeyContainsMasker(JsonMaskingConfig.getDefault(Set.of("someKey"))).mask(
                                prettyString)
                )
        );
    }
}
