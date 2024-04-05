package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class JsonMaskerTestUtil {
    private JsonMaskerTestUtil() {
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<JsonMaskerTestInstance> getJsonMaskerTestInstancesFromFile(String fileName) throws IOException {
        List<JsonMaskerTestInstance> testInstances = new ArrayList<>();
        ArrayNode jsonArray = mapper.readValue(JsonMaskerTestUtil.class.getClassLoader().getResource(fileName), ArrayNode.class);
        for (JsonNode jsonNode : jsonArray) {
            JsonMaskingConfig.Builder builder = JsonMaskingConfig.builder();
            JsonNode jsonMaskingConfig = jsonNode.findValue("maskingConfig");
            if (jsonMaskingConfig != null) {
                applyConfig(jsonMaskingConfig, builder);
            }
            JsonMaskingConfig maskingConfig = builder.build();
            var input = jsonNode.get("input").toString();
            var expectedOutput = jsonNode.get("expectedOutput").toString();
            testInstances.add(new JsonMaskerTestInstance(input, expectedOutput, new KeyContainsMasker(maskingConfig)));
        }
        return testInstances;
    }

    private static void applyConfig(JsonNode jsonMaskingConfig, JsonMaskingConfig.Builder builder) {
        jsonMaskingConfig.fields().forEachRemaining(e -> {
            String key = e.getKey();
            JsonNode value = e.getValue();
            switch (key) {
                case "maskKeys" -> builder.maskKeys(asSet(value, JsonNode::asText));
                case "maskJsonPaths" -> StreamSupport.stream(value.spliterator(), false).forEach(node -> {
                    if (node.isTextual()) {
                        builder.maskJsonPaths(Set.of(node.asText()));
                    } else {
                        builder.maskJsonPaths(asSet(node.get("keys"), JsonNode::asText), applyKeyConfig(node.get("keyMaskingConfig")));
                    }
                });
                case "allowKeys" -> builder.allowKeys(asSet(value, JsonNode::asText));
                case "allowJsonPaths" -> builder.allowJsonPaths(asSet(value, JsonNode::asText));
                case "caseSensitiveTargetKeys" -> {
                    if (value.booleanValue()) {
                        builder.caseSensitiveTargetKeys();
                    }
                }
                case "maskStringsWith" -> builder.maskStringsWith(value.textValue());
                case "maskStringCharactersWith" -> builder.maskStringCharactersWith(value.textValue());
                case "maskNumbersWith" -> {
                    if (value.isInt()) {
                        builder.maskNumbersWith(value.intValue());
                    } else {
                        builder.maskNumbersWith(value.textValue());
                    }
                }
                case "maskNumberDigitsWith" -> builder.maskNumberDigitsWith(value.intValue());
                case "maskBooleansWith" -> {
                    if (value.isBoolean()) {
                        builder.maskBooleansWith(value.booleanValue());
                    }
                    builder.maskBooleansWith(value.textValue());
                }
                default -> throw new IllegalArgumentException("Unknown option " + key);
            }
        });
    }

    private static KeyMaskingConfig applyKeyConfig(JsonNode jsonNode) {
        KeyMaskingConfig.Builder builder = KeyMaskingConfig.builder();
        jsonNode.fields().forEachRemaining(e -> {
            String key = e.getKey();
            JsonNode value = e.getValue();
            switch (key) {
                case "maskStringsWith" -> builder.maskStringsWith(value.textValue());
                case "maskStringCharactersWith" -> builder.maskStringCharactersWith(value.textValue());
                case "maskNumbersWith" -> {
                    if (value.isInt()) {
                        builder.maskNumbersWith(value.intValue());
                    } else {
                        builder.maskNumbersWith(value.textValue());
                    }
                }
                case "maskNumberDigitsWith" -> builder.maskNumberDigitsWith(value.intValue());
                case "maskBooleansWith" -> {
                    if (value.isBoolean()) {
                        builder.maskBooleansWith(value.booleanValue());
                    }
                    builder.maskBooleansWith(value.textValue());
                }
                default -> throw new IllegalArgumentException("Unknown option " + key);
            }
        });
        return builder.build();
    }

    private static <T> Set<T> asSet(JsonNode value, Function<JsonNode, T> mapper) {
        return StreamSupport.stream(value.spliterator(), false).map(mapper).collect(Collectors.toSet());
    }
}
