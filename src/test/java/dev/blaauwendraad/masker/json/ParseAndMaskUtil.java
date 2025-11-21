package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;
import dev.blaauwendraad.masker.json.path.JsonPath;
import dev.blaauwendraad.masker.json.util.ByteValueMaskerContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.NumericNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

import java.util.Set;
import java.util.stream.Collectors;

public final class ParseAndMaskUtil {

    public static final JsonMapper DEFAULT_JSON_MAPPER = new JsonMapper();

    private ParseAndMaskUtil() {
        // util
    }

    static JsonNode mask(String jsonString, JsonMaskingConfig jsonMaskingConfig) {
        return mask(DEFAULT_JSON_MAPPER.readTree(jsonString), jsonMaskingConfig);
    }

    static JsonNode mask(JsonNode jsonNode, JsonMaskingConfig jsonMaskingConfig) {
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
    ) {
        if (jsonNode instanceof ObjectNode objectNode) {
            var propertyNames = objectNode.propertyNames();
            for (String propertyName : propertyNames) {
                String jsonPathKey = currentJsonPath + "." + propertyName;
                String casingAppliedJsonPathKey = jsonMaskingConfig.caseSensitiveTargetKeys()
                        ? jsonPathKey
                        : jsonPathKey.toLowerCase();
                if ((jsonMaskingConfig.isInMaskMode()
                    && isTargetKey(casingAppliedJsonPathKey, casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys))
                    || (jsonMaskingConfig.isInAllowMode()
                       && !isTargetKey(casingAppliedJsonPathKey, casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys))) {
                    objectNode.replace(
                            propertyName,
                            maskJsonValue(
                                    objectNode.get(propertyName),
                                    jsonMaskingConfig.getConfig(propertyName),
                                    jsonMaskingConfig,
                                    casingAppliedTargetKeys
                            )
                    );
                } else if (!jsonMaskingConfig.isInAllowMode()
                           || !isTargetKey(casingAppliedJsonPathKey, casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys)) {
                    mask(jsonNode.get(propertyName), jsonMaskingConfig, jsonPathKey, casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys);
                }
            }
        } else if (jsonNode instanceof ArrayNode arrayNode) {
            String jsonPathKey = currentJsonPath + "[*]";
            String casingAppliedJsonPathKey = jsonMaskingConfig.caseSensitiveTargetKeys()
                    ? jsonPathKey
                    : jsonPathKey.toLowerCase();
            boolean mask = (jsonMaskingConfig.isInMaskMode()
                    && isTargetKey(casingAppliedJsonPathKey, casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys))
                    || (jsonMaskingConfig.isInAllowMode()
                    && !isTargetKey(casingAppliedJsonPathKey, casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys));
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
    ) {
        return switch (jsonNode.getNodeType()) {
            case STRING -> maskStringNode((StringNode) jsonNode, config);
            case NUMBER -> maskNumericNode((NumericNode) jsonNode, config);
            case BOOLEAN -> maskBooleanNode((BooleanNode) jsonNode, config);
            case ARRAY -> maskArrayNodeValue((ArrayNode) jsonNode, config, jsonMaskingConfig, casingAppliedTargetKeys);
            case OBJECT -> maskObjectNodeValue((ObjectNode) jsonNode, config, jsonMaskingConfig, casingAppliedTargetKeys);
            default -> jsonNode;
        };
    }

    private static JsonNode maskBooleanNode(BooleanNode booleanNode, KeyMaskingConfig config) {
        String maskedValue = ByteValueMaskerContext.maskBooleanWith(booleanNode.booleanValue(), config.getBooleanValueMasker());
        return DEFAULT_JSON_MAPPER.readTree(maskedValue);
    }

    private static JsonNode maskStringNode(StringNode stringNode, KeyMaskingConfig config) {
        // can't use testValue due to not preserving
        String stringRepresentation = stringNode.toString();
        String withoutQuotes = stringRepresentation.substring(1, stringRepresentation.length() - 1);
        String maskedValue = ByteValueMaskerContext.maskStringWith(withoutQuotes, config.getStringValueMasker());
        return DEFAULT_JSON_MAPPER.readTree(maskedValue);
    }

    private static JsonNode maskNumericNode(NumericNode numericNode, KeyMaskingConfig config) {
        String maskedValue = ByteValueMaskerContext.maskNumberWith(numericNode.numberValue(), config.getNumberValueMasker());
        return DEFAULT_JSON_MAPPER.readTree(maskedValue);
    }

    private static ArrayNode maskArrayNodeValue(
            ArrayNode arrayNode,
            KeyMaskingConfig config,
            JsonMaskingConfig jsonMaskingConfig,
            Set<String> casingAppliedTargetKeys
    ) {
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
    ) {
        ObjectNode maskedObjectNode = JsonNodeFactory.instance.objectNode();

        objectNode.forEachEntry((key, value) -> {
            if (jsonMaskingConfig.isInAllowMode()
                    && casingAppliedTargetKeys.contains(jsonMaskingConfig.caseSensitiveTargetKeys() ? key : key.toLowerCase())) {
                // field is explicitly allowed, so just put the original field back
                maskedObjectNode.set(key, value);
            } else {
                maskedObjectNode.set(key, maskJsonValue(value, config, jsonMaskingConfig, casingAppliedTargetKeys));
            }
        });
        return maskedObjectNode;
    }

}
