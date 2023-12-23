package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Set;

public final class ParseAndMaskUtil {

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();

    private ParseAndMaskUtil() {
        // util
    }

    @Nonnull
    static JsonNode mask(String jsonString, JsonMaskingConfig jsonMaskingConfig) throws JsonProcessingException {
        return mask(DEFAULT_OBJECT_MAPPER.readTree(jsonString), jsonMaskingConfig);
    }

    @Nonnull
    static JsonNode mask(JsonNode jsonNode, JsonMaskingConfig jsonMaskingConfig) {
        Set<String> targetKeys = jsonMaskingConfig.getTargetKeys();
        if (targetKeys.isEmpty()) {
            return jsonNode;
        }
        if (jsonNode instanceof ObjectNode objectNode) {
            objectNode.fieldNames().forEachRemaining(
                    key -> {
                        if (targetKeys.contains(key)) {
                            objectNode.replace(key, maskJsonValue(objectNode.get(key), targetKeys));
                        } else {
                            mask(jsonNode.get(key), jsonMaskingConfig);
                        }
                    }
            );
        } else if (jsonNode instanceof ArrayNode arrayNode) {
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode originalElement = arrayNode.get(i);
                JsonNode newElement = mask(originalElement, jsonMaskingConfig);
                arrayNode.set(i, newElement);
            }
        }
        return jsonNode;
    }

    @Nonnull
    private static JsonNode maskJsonValue(JsonNode jsonNode, Set<String> targetKeys) {
        return switch (jsonNode.getNodeType()) {
            case STRING -> maskTextNode((TextNode) jsonNode);
            case ARRAY -> maskArrayNodeValue((ArrayNode) jsonNode, targetKeys);
            case OBJECT -> maskObjectNodeValue((ObjectNode) jsonNode, targetKeys);
            default -> jsonNode;
        };
    }

    @Nonnull
    private static TextNode maskTextNode(TextNode textNode) {
        return new TextNode(maskText(textNode.textValue()));
    }

    @Nonnull
    private static ArrayNode maskArrayNodeValue(ArrayNode arrayNode, Set<String> targetKeys) {
        ArrayNode maskedArrayNode = JsonNodeFactory.instance.arrayNode();
        for (JsonNode element : arrayNode) {
            maskedArrayNode.add(maskJsonValue(element, targetKeys));
        }
        return maskedArrayNode;
    }

    @Nonnull
    private static ObjectNode maskObjectNodeValue(ObjectNode objectNode, Set<String> targetKeys) {
        ObjectNode maskedObjectNode = JsonNodeFactory.instance.objectNode();
        Iterator<String> fieldNames = objectNode.fieldNames();

        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode fieldValue = objectNode.get(fieldName);
            maskedObjectNode.set(fieldName, maskJsonValue(fieldValue, targetKeys));
        }
        return maskedObjectNode;
    }

    @Nonnull
    private static String maskText(String text) {
        return "*".repeat(text.length());
    }
}
