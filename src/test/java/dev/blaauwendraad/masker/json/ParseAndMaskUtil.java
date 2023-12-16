package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;

import java.io.IOException;
import java.util.Set;

public final class ParseAndMaskUtil {
    private ParseAndMaskUtil() {
        // util
    }

    static JsonNode mask(byte[] jsonAsBytes,
                         String targetKeys,
                         JsonMaskingConfig.TargetKeyMode targetKeyMode,
                         ObjectMapper mapper) throws IOException {
        return mask(jsonAsBytes, Set.of(targetKeys), targetKeyMode, mapper);
    }

    static JsonNode mask(byte[] jsonAsBytes,
                         Set<String> targetKeys,
                         JsonMaskingConfig.TargetKeyMode targetKeyMode,
                         ObjectMapper mapper) throws IOException {
        return mask(mapper.readValue(jsonAsBytes, JsonNode.class), targetKeys, targetKeyMode);
    }

    static JsonNode mask(String jsonAsString,
                         String targetKeys,
                         JsonMaskingConfig.TargetKeyMode targetKeyMode,
                         ObjectMapper mapper) throws JsonProcessingException {
        return mask(jsonAsString, Set.of(targetKeys), targetKeyMode, mapper);
    }

    static JsonNode mask(String jsonAsString,
                         Set<String> targetKeys,
                         JsonMaskingConfig.TargetKeyMode targetKeyMode,
                         ObjectMapper mapper) throws JsonProcessingException {
        return mask(mapper.readValue(jsonAsString, JsonNode.class), targetKeys, targetKeyMode);
    }

    static JsonNode mask(JsonNode jsonNode, Set<String> targetKeys, JsonMaskingConfig.TargetKeyMode targetKeyMode) {
        boolean maskMode = targetKeyMode == JsonMaskingConfig.TargetKeyMode.MASK;
        // Now, recursively invoke this method on all nodes
        if (jsonNode.isArray()) {
            for (JsonNode childNode : jsonNode) {
                mask(childNode, targetKeys, targetKeyMode);
            }
        } else if (jsonNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            jsonNode.fieldNames().forEachRemaining(key -> {
                JsonNode childNode = jsonNode.get(key);
                if (childNode instanceof TextNode) { // TODO: probably needs to be ValueNode? Why is it working in numeric tests?
                    boolean keyMatched = targetKeys.contains(key);
                    if ((maskMode && keyMatched) || (!maskMode && !keyMatched)) {
                        String replacementValue = "*".repeat(childNode.textValue().length());
                        objectNode.put(key, replacementValue);
                    }
                } else if (childNode instanceof NumericNode) {
                    // TODO mask numbers as well
                }
                mask(childNode, targetKeys, targetKeyMode);
            });
        }
        return jsonNode;
    }
}
