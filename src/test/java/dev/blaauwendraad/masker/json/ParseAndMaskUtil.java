package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.path.JsonPath;

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

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
        return mask(jsonNode, jsonMaskingConfig, "$");
    }

    @Nonnull
    static JsonNode mask(JsonNode jsonNode, JsonMaskingConfig jsonMaskingConfig, String currentJsonPath) {
        Set<String> casingAppliedTargetKeys;
        Set<JsonPath> casingAppliedTargetJsonPathKeys;
        if (jsonMaskingConfig.caseSensitiveTargetKeys()) {
            casingAppliedTargetKeys = jsonMaskingConfig.getTargetKeys();
            casingAppliedTargetJsonPathKeys = jsonMaskingConfig.getTargetJsonPaths();
        } else {
            casingAppliedTargetKeys = jsonMaskingConfig.getTargetKeys()
                    .stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            casingAppliedTargetJsonPathKeys = jsonMaskingConfig.getTargetJsonPaths()
                    .stream()
                    .map(JsonPath::toString)
                    .map(String::toLowerCase)
                    .map(JsonPath::from)
                    .collect(Collectors.toSet());

        }
        if (casingAppliedTargetKeys.isEmpty()) {
            return jsonNode;
        }
        if (jsonNode instanceof ObjectNode objectNode) {
            objectNode.fieldNames().forEachRemaining(
                    key -> {
                        String jsonPathKey = currentJsonPath + "." + key;
                        String casingAppliedJsonPathKey = jsonMaskingConfig.caseSensitiveTargetKeys()
                                ? jsonPathKey
                                : jsonPathKey.toLowerCase();
                        if (jsonMaskingConfig.isInMaskMode()
                                && isTargetKey(casingAppliedJsonPathKey, casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys)
                                || jsonMaskingConfig.isInAllowMode()
                                && !isTargetKey(casingAppliedJsonPathKey, casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys)) {
                            objectNode.replace(
                                    key,
                                    maskJsonValue(
                                            objectNode.get(key),
                                            jsonMaskingConfig,
                                            casingAppliedTargetKeys
                                    )
                            );
                        } else if (!jsonMaskingConfig.isInAllowMode()
                                || !isTargetKey(casingAppliedJsonPathKey, casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys)) {
                            mask(jsonNode.get(key), jsonMaskingConfig, jsonPathKey);
                        }
                    }
            );
        } else if (jsonNode instanceof ArrayNode arrayNode) {
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode originalElement = arrayNode.get(i);
                JsonNode newElement = mask(originalElement, jsonMaskingConfig, currentJsonPath + "[" + i + "]");
                arrayNode.set(i, newElement);
            }
        }
        return jsonNode;
    }

    private static boolean isTargetKey(String jsonPathKey, Set<String> targetKeys, Set<JsonPath> targetJsonPathKeys) {
        return targetKeys.contains(jsonPathKey.substring(jsonPathKey.lastIndexOf('.') + 1))
                || targetJsonPathKeys.contains(JsonPath.from(jsonPathKey));
    }

    @Nonnull
    private static JsonNode maskJsonValue(
            JsonNode jsonNode,
            JsonMaskingConfig jsonMaskingConfig,
            Set<String> casingAppliedTargetKeys
    ) {
        return switch (jsonNode.getNodeType()) {
            case STRING -> maskTextNode((TextNode) jsonNode, jsonMaskingConfig);
            case NUMBER -> maskNumericNode((NumericNode) jsonNode, jsonMaskingConfig);
            case ARRAY -> maskArrayNodeValue((ArrayNode) jsonNode, jsonMaskingConfig, casingAppliedTargetKeys);
            case OBJECT -> maskObjectNodeValue((ObjectNode) jsonNode, jsonMaskingConfig, casingAppliedTargetKeys);
            default -> jsonNode;
        };
    }

    @Nonnull
    private static TextNode maskTextNode(TextNode textNode, JsonMaskingConfig jsonMaskingConfig) {
        return new TextNode(maskText(textNode.textValue(), jsonMaskingConfig));
    }

    @Nonnull
    private static NumericNode maskNumericNode(NumericNode numericNode, JsonMaskingConfig jsonMaskingConfig) {
        if (!jsonMaskingConfig.isNumberMaskingEnabled()) {
            return numericNode;
        }
        String text = numericNode.asText();
        int numericLength = jsonMaskingConfig.isLengthObfuscationEnabled()
                ? jsonMaskingConfig.getObfuscationLength()
                : text.length();
        int theNumber = jsonMaskingConfig.getMaskNumericValuesWith();
        String repeatingNumber = String.valueOf(theNumber).repeat(numericLength);
        return new BigIntegerNode(new BigInteger(repeatingNumber));
    }

    @Nonnull
    private static ArrayNode maskArrayNodeValue(
            ArrayNode arrayNode,
            JsonMaskingConfig jsonMaskingConfig,
            Set<String> casingAppliedTargetKeys
    ) {
        ArrayNode maskedArrayNode = JsonNodeFactory.instance.arrayNode();
        for (JsonNode element : arrayNode) {
            maskedArrayNode.add(maskJsonValue(element, jsonMaskingConfig, casingAppliedTargetKeys));
        }
        return maskedArrayNode;
    }

    @Nonnull
    private static ObjectNode maskObjectNodeValue(
            ObjectNode objectNode,
            JsonMaskingConfig jsonMaskingConfig,
            Set<String> casingAppliedTargetKeys
    ) {
        ObjectNode maskedObjectNode = JsonNodeFactory.instance.objectNode();
        Iterator<String> fieldNames = objectNode.fieldNames();

        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (jsonMaskingConfig.isInAllowMode() && casingAppliedTargetKeys
                    .contains(jsonMaskingConfig.caseSensitiveTargetKeys() ? fieldName : fieldName.toLowerCase())) {
                // field is explicitly allowed, so just put the original field back
                maskedObjectNode.set(fieldName, objectNode.get(fieldName));
            } else {
                JsonNode fieldValue = objectNode.get(fieldName);
                maskedObjectNode.set(fieldName, maskJsonValue(fieldValue, jsonMaskingConfig, casingAppliedTargetKeys));
            }
        }
        return maskedObjectNode;
    }

    @Nonnull
    private static String maskText(String text, JsonMaskingConfig jsonMaskingConfig) {
        int numberOfAsterisks = jsonMaskingConfig.isLengthObfuscationEnabled()
                ? jsonMaskingConfig.getObfuscationLength()
                : text.length();
        return "*".repeat(numberOfAsterisks);
    }
}
