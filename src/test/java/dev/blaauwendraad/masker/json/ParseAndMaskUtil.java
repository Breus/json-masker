package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;
import dev.blaauwendraad.masker.json.path.JsonPath;
import dev.blaauwendraad.masker.json.path.JsonPathParser;

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public final class ParseAndMaskUtil {

    public static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();
    private static final JsonPathParser JSON_PATH_PARSER = new JsonPathParser();

    private ParseAndMaskUtil() {
        // util
    }

    @Nonnull
    static JsonNode mask(String jsonString, JsonMaskingConfig jsonMaskingConfig) throws JsonProcessingException {
        return mask(DEFAULT_OBJECT_MAPPER.readTree(jsonString), jsonMaskingConfig);
    }

    @Nonnull
    static JsonNode mask(JsonNode jsonNode, JsonMaskingConfig jsonMaskingConfig) {
        if (jsonMaskingConfig.isInAllowMode() && !jsonNode.isArray() && !jsonNode.isObject()) {
            return maskJsonValue(jsonNode, jsonMaskingConfig.getDefaultConfig(), jsonMaskingConfig, jsonMaskingConfig.getTargetKeys());
        }
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
            JsonPathParser jsonPathParser = new JsonPathParser();
            casingAppliedTargetKeys = jsonMaskingConfig.getTargetKeys()
                    .stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            casingAppliedTargetJsonPathKeys = jsonMaskingConfig.getTargetJsonPaths()
                    .stream()
                    .map(JsonPath::toString)
                    .map(String::toLowerCase)
                    .map(jsonPathParser::parse)
                    .collect(Collectors.toSet());

        }
        if (casingAppliedTargetKeys.isEmpty() && casingAppliedTargetJsonPathKeys.isEmpty()) {
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
                                            jsonMaskingConfig.getConfig(key),
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
                String jsonPathKey = currentJsonPath + "[" + i + "]";
                String casingAppliedJsonPathKey = jsonMaskingConfig.caseSensitiveTargetKeys()
                        ? jsonPathKey
                        : jsonPathKey.toLowerCase();

                if (jsonMaskingConfig.isInMaskMode()
                        && isTargetKey(casingAppliedJsonPathKey, casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys)
                        || jsonMaskingConfig.isInAllowMode()
                        && !isTargetKey(casingAppliedJsonPathKey, casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys)) {
                    arrayNode.set(
                            i,
                            maskJsonValue(
                                    arrayNode.get(i),
                                    jsonMaskingConfig.getDefaultConfig(),
                                    jsonMaskingConfig,
                                    casingAppliedTargetKeys
                            )
                    );
                } else if (!jsonMaskingConfig.isInAllowMode()
                        || !isTargetKey(casingAppliedJsonPathKey, casingAppliedTargetKeys, casingAppliedTargetJsonPathKeys)) {
                    mask(arrayNode.get(i), jsonMaskingConfig, jsonPathKey);
                }
            }
        }
        return jsonNode;
    }

    private static boolean isTargetKey(String jsonPathKey, Set<String> targetKeys, Set<JsonPath> targetJsonPathKeys) {
        return targetKeys.contains(jsonPathKey.substring(jsonPathKey.lastIndexOf('.') + 1))
                || targetJsonPathKeys.contains(JSON_PATH_PARSER.tryParse(jsonPathKey));
    }

    @Nonnull
    private static JsonNode maskJsonValue(
            JsonNode jsonNode,
            KeyMaskingConfig config,
            JsonMaskingConfig jsonMaskingConfig,
            Set<String> casingAppliedTargetKeys
    ) {
        return switch (jsonNode.getNodeType()) {
            case STRING -> maskTextNode((TextNode) jsonNode, config);
            case NUMBER -> maskNumericNode((NumericNode) jsonNode, config);
            case BOOLEAN -> maskBooleanNode((BooleanNode) jsonNode, config);
            case ARRAY -> maskArrayNodeValue((ArrayNode) jsonNode, config, jsonMaskingConfig, casingAppliedTargetKeys);
            case OBJECT -> maskObjectNodeValue((ObjectNode) jsonNode, config, jsonMaskingConfig, casingAppliedTargetKeys);
            default -> jsonNode;
        };
    }

    private static JsonNode maskBooleanNode(BooleanNode booleanNode, KeyMaskingConfig config) {
        if (config.isDisableBooleanMasking()) {
            return booleanNode;
        }
        if (config.getMaskBooleansWith() == null) {
            throw new IllegalArgumentException("Invalid masking configuration for key: " + config);
        }
        String maskBooleansWith = new String(config.getMaskBooleansWith(), StandardCharsets.UTF_8);
        if (maskBooleansWith.startsWith("\"")) {
            return new TextNode(maskBooleansWith.substring(1, maskBooleansWith.length() - 1));
        } else {
            return BooleanNode.valueOf(Boolean.parseBoolean(maskBooleansWith));
        }
    }

    @Nonnull
    private static TextNode maskTextNode(TextNode textNode, KeyMaskingConfig config) {
        String text = textNode.textValue();
        if (config.getMaskStringsWith() != null) {
            text = new String(config.getMaskStringsWith(), StandardCharsets.UTF_8);
        } else if (config.getMaskStringCharactersWith() != null) {
            text = new String(config.getMaskStringCharactersWith(), StandardCharsets.UTF_8).repeat(text.length());
        } else {
            throw new IllegalArgumentException("Invalid masking configuration for key: " + config);
        }
        return new TextNode(text);
    }

    @Nonnull
    private static ValueNode maskNumericNode(NumericNode numericNode, KeyMaskingConfig config) {
        if (config.isDisableNumberMasking()) {
            return numericNode;
        }
        String text = numericNode.asText();
        if (config.getMaskNumbersWith() != null) {
            String maskNumbersWith = new String(config.getMaskNumbersWith(), StandardCharsets.UTF_8);
            if (maskNumbersWith.startsWith("\"")) {
                return new TextNode(maskNumbersWith.substring(1, maskNumbersWith.length() - 1));
            } else {
                return new BigIntegerNode(BigInteger.valueOf(Long.parseLong(maskNumbersWith)));
            }
        } else if (config.getMaskNumberDigitsWith() != null) {
            int maskNumberDigitsWith = Integer.parseInt(new String(config.getMaskNumberDigitsWith(), StandardCharsets.UTF_8));
            BigInteger mask = BigInteger.valueOf(maskNumberDigitsWith);
            for (int i = 1; i < text.length(); i++) {
                mask = mask.multiply(BigInteger.TEN);
                mask = mask.add(BigInteger.valueOf(maskNumberDigitsWith));
            }
            return new BigIntegerNode(mask);
        } else {
            throw new IllegalArgumentException("Invalid masking configuration for key: " + config);
        }
    }

    @Nonnull
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

    @Nonnull
    private static ObjectNode maskObjectNodeValue(
            ObjectNode objectNode,
            KeyMaskingConfig config,
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
                maskedObjectNode.set(fieldName, maskJsonValue(fieldValue, config, jsonMaskingConfig, casingAppliedTargetKeys));
            }
        }
        return maskedObjectNode;
    }

}
