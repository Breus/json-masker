package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;
import dev.blaauwendraad.masker.json.path.JsonPath;
import dev.blaauwendraad.masker.json.util.ByteValueMaskerContext;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public final class ParseAndMaskUtil {

    public static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();

    private ParseAndMaskUtil() {
        // util
    }

    static JsonNode mask(String jsonString, JsonMaskingConfig jsonMaskingConfig) throws JsonProcessingException {
        return mask(DEFAULT_OBJECT_MAPPER.readTree(jsonString), jsonMaskingConfig);
    }

    static JsonNode mask(JsonNode jsonNode, JsonMaskingConfig jsonMaskingConfig) throws JsonProcessingException {
        if (jsonMaskingConfig.isInAllowMode() && !jsonNode.isArray() && !jsonNode.isObject()) {
            return maskJsonValue(jsonNode, jsonMaskingConfig.getDefaultConfig(), jsonMaskingConfig, jsonMaskingConfig.getTargetKeys());
        }
        Set<String> casingAppliedTargetKeys;
        Set<String> casingAppliedTargetJsonPathKeys;
        if (jsonMaskingConfig.caseSensitiveTargetKeys()) {
            casingAppliedTargetKeys = jsonMaskingConfig.getTargetKeys();
            casingAppliedTargetJsonPathKeys = jsonMaskingConfig.getTargetJsonPaths()
                    .stream()
                    .map(JsonPath::toString)
                    .collect(Collectors.toSet());
        } else {
            casingAppliedTargetKeys = jsonMaskingConfig.getTargetKeys()
                    .stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            casingAppliedTargetJsonPathKeys = jsonMaskingConfig.getTargetJsonPaths()
                    .stream()
                    .map(JsonPath::toString)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

        }
        return mask(jsonNode, jsonMaskingConfig, "$", casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys);
    }

    static JsonNode mask(
            JsonNode jsonNode,
            JsonMaskingConfig jsonMaskingConfig,
            String currentJsonPath,
            Set<String> casingAppliedTargetKeys,
            Set<String> casingAppliedTargetJsonPathKeys
    ) throws JsonProcessingException {
        if (jsonNode instanceof ObjectNode objectNode) {
            Iterable<String> fieldNames = objectNode::fieldNames;
            for (String fieldName : fieldNames) {
                String jsonPathKey = currentJsonPath + "." + fieldName;
                String casingAppliedJsonPathKey = jsonMaskingConfig.caseSensitiveTargetKeys()
                        ? jsonPathKey
                        : jsonPathKey.toLowerCase();
                if (jsonMaskingConfig.isInMaskMode()
                    && isTargetKey(casingAppliedJsonPathKey, casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys)
                    || jsonMaskingConfig.isInAllowMode()
                       && !isTargetKey(casingAppliedJsonPathKey, casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys)) {
                    objectNode.replace(
                            fieldName,
                            maskJsonValue(
                                    objectNode.get(fieldName),
                                    jsonMaskingConfig.getConfig(fieldName),
                                    jsonMaskingConfig,
                                    casingAppliedTargetKeys
                            )
                    );
                } else if (!jsonMaskingConfig.isInAllowMode()
                           || !isTargetKey(casingAppliedJsonPathKey, casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys)) {
                    mask(jsonNode.get(fieldName), jsonMaskingConfig, jsonPathKey, casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys);
                }
            }
        } else if (jsonNode instanceof ArrayNode arrayNode) {
            String jsonPathKey = currentJsonPath + "[*]";
            String casingAppliedJsonPathKey = jsonMaskingConfig.caseSensitiveTargetKeys()
                    ? jsonPathKey
                    : jsonPathKey.toLowerCase();
            boolean mask = jsonMaskingConfig.isInMaskMode()
                    && isTargetKey(casingAppliedJsonPathKey, casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys)
                    || jsonMaskingConfig.isInAllowMode()
                    && !isTargetKey(casingAppliedJsonPathKey, casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys);
            boolean visit = !jsonMaskingConfig.isInAllowMode()
                    || !isTargetKey(casingAppliedJsonPathKey, casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys);
            for (int i = 0; i < arrayNode.size(); i++) {
                if (mask) {
                    arrayNode.set(
                            i,
                            maskJsonValue(
                                    arrayNode.get(i),
                                    jsonMaskingConfig.getDefaultConfig(),
                                    jsonMaskingConfig,
                                    casingAppliedTargetKeys
                            )
                    );
                } else if (visit) {
                    mask(arrayNode.get(i), jsonMaskingConfig, jsonPathKey, casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys);
                }
            }
        }
        return jsonNode;
    }

    private static boolean isTargetKey(String jsonPathKey, Set<String> targetKeys, Set<String> targetJsonPathKeys) {
        return targetKeys.contains(jsonPathKey.substring(jsonPathKey.lastIndexOf('.') + 1))
                || targetJsonPathKeys.contains(jsonPathKey);
    }

    private static JsonNode maskJsonValue(
            JsonNode jsonNode,
            KeyMaskingConfig config,
            JsonMaskingConfig jsonMaskingConfig,
            Set<String> casingAppliedTargetKeys
    ) throws JsonProcessingException {
        return switch (jsonNode.getNodeType()) {
            case STRING -> maskTextNode((TextNode) jsonNode, config);
            case NUMBER -> maskNumericNode((NumericNode) jsonNode, config);
            case BOOLEAN -> maskBooleanNode((BooleanNode) jsonNode, config);
            case ARRAY -> maskArrayNodeValue((ArrayNode) jsonNode, config, jsonMaskingConfig, casingAppliedTargetKeys);
            case OBJECT -> maskObjectNodeValue((ObjectNode) jsonNode, config, jsonMaskingConfig, casingAppliedTargetKeys);
            default -> jsonNode;
        };
    }

    private static JsonNode maskBooleanNode(BooleanNode booleanNode, KeyMaskingConfig config) throws JsonProcessingException {
        String maskedValue = ByteValueMaskerContext.maskBooleanWith(booleanNode.booleanValue(), config.getBooleanValueMasker());
        return DEFAULT_OBJECT_MAPPER.readTree(maskedValue);
    }

    private static JsonNode maskTextNode(TextNode textNode, KeyMaskingConfig config) throws JsonProcessingException {
        // can't use testValue due to not preserving
        String stringRepresentation = textNode.toString();
        String withoutQuotes = stringRepresentation.substring(1, stringRepresentation.length() - 1);
        String maskedValue = ByteValueMaskerContext.maskStringWith(withoutQuotes, config.getStringValueMasker());
        return DEFAULT_OBJECT_MAPPER.readTree(maskedValue);
    }

    private static JsonNode maskNumericNode(NumericNode numericNode, KeyMaskingConfig config) throws JsonProcessingException {
        String maskedValue = ByteValueMaskerContext.maskNumberWith(numericNode.numberValue(), config.getNumberValueMasker());
        return DEFAULT_OBJECT_MAPPER.readTree(maskedValue);
    }

    private static ArrayNode maskArrayNodeValue(
            ArrayNode arrayNode,
            KeyMaskingConfig config,
            JsonMaskingConfig jsonMaskingConfig,
            Set<String> casingAppliedTargetKeys
    ) throws JsonProcessingException {
        ArrayNode maskedArrayNode = JsonNodeFactory.instance.arrayNode();
        for (JsonNode element : arrayNode) {
            maskedArrayNode.add(maskJsonValue(element, config, jsonMaskingConfig, casingAppliedTargetKeys));
        }
        return maskedArrayNode;
    }

    private static ObjectNode maskObjectNodeValue(
            ObjectNode objectNode,
            KeyMaskingConfig config,
            JsonMaskingConfig jsonMaskingConfig,
            Set<String> casingAppliedTargetKeys
    ) throws JsonProcessingException {
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
                maskedObjectNode.set(fieldName, maskJsonValue(fieldValue, config, jsonMaskingConfig, casingAppliedTargetKeys));
            }
        }
        return maskedObjectNode;
    }

}
