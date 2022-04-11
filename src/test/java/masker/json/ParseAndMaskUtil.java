package masker.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Set;

public final class ParseAndMaskUtil {
    private ParseAndMaskUtil() {
        // util
    }

    static ObjectNode parseBytesAndMask(byte[] jsonAsBytes, Set<String> keysToBeMasked, ObjectMapper mapper) throws IOException {
        ObjectNode objectNode = mapper.readValue(jsonAsBytes, ObjectNode.class);
        for (String keyToBeMasked : keysToBeMasked) {
            maskPropertyInObjectNode(objectNode, keyToBeMasked, mapper);
        }
        return objectNode;
    }

    static ObjectNode parseBytesAndMask(byte[] jsonAsBytes, String keyToBeMasked, ObjectMapper mapper) throws IOException {
        return maskPropertyInObjectNode(mapper.readValue(jsonAsBytes, ObjectNode.class), keyToBeMasked, mapper);
    }

    static ObjectNode parseStringAndMask(String jsonAsString, String keyToBeMasked, ObjectMapper mapper) throws JsonProcessingException {
        return maskPropertyInObjectNode(mapper.readValue(jsonAsString, ObjectNode.class), keyToBeMasked, mapper);
    }

    static ObjectNode maskPropertyInObjectNode(ObjectNode objectNode, String propertyKey, ObjectMapper mapper) {
        JsonNode jsonNode = objectNode.findValue(propertyKey);
        if (jsonNode != null) {
            objectNode.set(propertyKey, mapper.convertValue(getMask(jsonNode.textValue()), JsonNode.class));
        }
        return objectNode;
    }

    private static String getMask(String value) {
        return "*".repeat(value.length());
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
