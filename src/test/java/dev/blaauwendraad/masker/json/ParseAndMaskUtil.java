package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.util.Set;

public final class ParseAndMaskUtil {
    private ParseAndMaskUtil() {
        // util
    }

    static JsonNode parseBytesAndMask(byte[] jsonAsBytes,
                                      Set<String> keysToBeMasked,
                                      ObjectMapper mapper) throws IOException {
        JsonNode jsonNode = mapper.readValue(jsonAsBytes, JsonNode.class);
        for (String keyToBeMasked : keysToBeMasked) {
            maskPropertyInJsonNode(jsonNode, keyToBeMasked);
        }
        return jsonNode;
    }

    static JsonNode parseBytesAndMask(byte[] jsonAsBytes,
                                      String keyToBeMasked,
                                      ObjectMapper mapper) throws IOException {
        return mask(mapper.readValue(jsonAsBytes, JsonNode.class), keyToBeMasked);
    }

    static JsonNode parseStringAndMask(String jsonAsString,
                                       String keyToBeMasked,
                                       ObjectMapper mapper) throws JsonProcessingException {
        return mask(mapper.readValue(jsonAsString, JsonNode.class), keyToBeMasked);
    }


    static JsonNode mask(JsonNode jsonNode, String keyToBeMasked) {
        maskPropertyInJsonNode(jsonNode, keyToBeMasked);
        return jsonNode;
    }

    static JsonNode mask(JsonNode jsonNode, Set<String> keysToBeMasked) {
        maskPropertiesInJsonNode(jsonNode, keysToBeMasked);
        return jsonNode;
    }

    static void maskPropertiesInJsonNode(JsonNode parent, Set<String> keysToBeMasked) {
        for (String keyToBeMasked : keysToBeMasked) {
            maskPropertyInJsonNode(parent, keyToBeMasked);
        }
    }

    static void maskPropertyInJsonNode(JsonNode parent, String keyToBeMasked) {
        JsonNode jsonNode = parent.get(keyToBeMasked);
        if (jsonNode instanceof TextNode) {
            String replacementValue = "*".repeat(jsonNode.textValue().length());
            ((ObjectNode) parent).put(keyToBeMasked, replacementValue);
        }
        // Now, recursively invoke this method on all nodes
        for (JsonNode child : parent) {
            maskPropertyInJsonNode(child, keyToBeMasked);
        }
    }

    static String readJsonFromFileAsString(String resourceName, Class<?> clazz) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonObject;
        try {
            jsonObject = mapper.readValue(clazz.getClassLoader().getResource(resourceName), ObjectNode.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read benchmark from input file");
        }
        return jsonObject.toString();
    }
}
