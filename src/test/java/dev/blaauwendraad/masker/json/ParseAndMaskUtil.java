package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

public final class ParseAndMaskUtil {
    private ParseAndMaskUtil() {
        // util
    }

    static JsonNode mask(
            byte[] jsonAsBytes,
            String targetKeys,
            JsonMaskingConfig.TargetKeyMode targetKeyMode,
            ObjectMapper mapper
    ) throws IOException {
        return mask(jsonAsBytes, Set.of(targetKeys), targetKeyMode, mapper);
    }

    static JsonNode mask(
            byte[] jsonAsBytes,
            Set<String> targetKeys,
            JsonMaskingConfig.TargetKeyMode targetKeyMode,
            ObjectMapper mapper
    ) throws IOException {
        return mask(mapper.readValue(jsonAsBytes, JsonNode.class), targetKeys, targetKeyMode);
    }

    static JsonNode mask(
            String jsonAsString,
            String targetKeys,
            JsonMaskingConfig.TargetKeyMode targetKeyMode,
            ObjectMapper mapper
    ) throws JsonProcessingException {
        return mask(jsonAsString, Set.of(targetKeys), targetKeyMode, mapper);
    }

    static JsonNode mask(
            String jsonAsString,
            Set<String> targetKeys,
            JsonMaskingConfig.TargetKeyMode targetKeyMode,
            ObjectMapper mapper
    ) throws JsonProcessingException {
        return mask(mapper.readValue(jsonAsString, JsonNode.class), targetKeys, targetKeyMode);
    }

    static JsonNode mask(JsonNode rootNode, Set<String> targetKeys, JsonMaskingConfig.TargetKeyMode targetKeyMode) {
        if (targetKeys.isEmpty()) {
            return rootNode;
        }
        return maskJsonNode(rootNode, targetKeys);
    }

    public static JsonNode maskJsonNode(JsonNode jsonNode, Set<String> targetKeys) {
        if (targetKeys.isEmpty()) {
            return jsonNode;
        }
        if (jsonNode instanceof ObjectNode objectNode) {
            objectNode.fieldNames().forEachRemaining(
                    key -> {
                        if (targetKeys.contains(key)) {
                            objectNode.replace(key, maskJsonValue(objectNode.get(key), targetKeys));
                        } else {
                            maskJsonNode(jsonNode.get(key), targetKeys);
                        }
                    }
            );
        }
        if (jsonNode instanceof ArrayNode arrayNode) {
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode originalElement = arrayNode.get(i);

                // Replace the element with the output of a method (e.g., doubling the value)
                JsonNode newElement = maskJsonNode(originalElement, targetKeys);

                // Set the new element in the array
                arrayNode.set(i, newElement);
            }
        }
        return jsonNode;
    }

    private static JsonNode maskJsonValue(JsonNode jsonNode, Set<String> targetKeys) {
        return switch (jsonNode.getNodeType()) {
            case STRING -> maskTextNode((TextNode) jsonNode);
            case ARRAY -> maskArrayNodeValue((ArrayNode) jsonNode, targetKeys);
            case OBJECT -> maskObjectNodeValue((ObjectNode) jsonNode, targetKeys);
            default -> jsonNode;
        };
    }

    private static TextNode maskTextNode(TextNode textNode) {
        return new TextNode(maskText(textNode.textValue()));
    }

    private static ArrayNode maskArrayNodeValue(ArrayNode arrayNode, Set<String> targetKeys) {
        ArrayNode maskedArrayNode = JsonNodeFactory.instance.arrayNode();
        for (JsonNode element : arrayNode) {
            maskedArrayNode.add(maskJsonValue(element, targetKeys));
        }
        return maskedArrayNode;
    }

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

    private static String maskText(String text) {
        return "*".repeat(text.length());
    }
}
