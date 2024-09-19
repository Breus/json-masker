package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfigTestUtil;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class JsonMaskerTestUtil {
    private static final int MINIMAL_STREAMING_BUFFER_SIZE = 5;

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
            var input = jsonNode.get("input").toPrettyString();
            var expectedOutput = jsonNode.get("expectedOutput").toPrettyString();
            testInstances.add(new JsonMaskerTestInstance(input, expectedOutput, new KeyContainsMasker(maskingConfig)));
        }
        return testInstances;
    }

    private static void applyConfig(JsonNode jsonMaskingConfig, JsonMaskingConfig.Builder builder) {
        jsonMaskingConfig.fields().forEachRemaining(e -> {
            String key = e.getKey();
            JsonNode value = e.getValue();
            switch (key) {
                case "maskKeys" -> StreamSupport.stream(value.spliterator(), false).forEach(node -> {
                    if (node.isTextual()) {
                        builder.maskKeys(Set.of(node.asText()));
                    } else {
                        builder.maskKeys(asSet(node.get("keys"), JsonNode::asText), applyKeyConfig(node.get("keyMaskingConfig")));
                    }
                });
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

    /**
     * Asserts that JsonMasker result matches the expected output and is the same when using bytes mode,
     * streaming mode and streaming mode with minimal buffer size.
     *
     * @param jsonMasker an instance of JsonMasker
     * @param input the input JSON
     */
    public static void assertJsonMaskerApiEquivalence(JsonMasker jsonMasker,
                                                      String input,
                                                      @Nullable String expectedOutput,
                                                      boolean pretty) {
        String bytesOutput = jsonMasker.mask(input);
        String streamsOutput = getStreamingModeOutput(jsonMasker, input);
        int oldBufferSize = ((KeyContainsMasker) jsonMasker).maskingConfig.bufferSize();
        JsonMaskingConfigTestUtil.setBufferSize(((KeyContainsMasker) jsonMasker).maskingConfig, MINIMAL_STREAMING_BUFFER_SIZE);
        String minimalBufferStreamsOutput = getStreamingModeOutput(jsonMasker, input);
        JsonMaskingConfigTestUtil.setBufferSize(((KeyContainsMasker) jsonMasker).maskingConfig, oldBufferSize);
        if (pretty) {
            try {
                bytesOutput = ParseAndMaskUtil.DEFAULT_OBJECT_MAPPER.readTree(bytesOutput).toString();
                streamsOutput = ParseAndMaskUtil.DEFAULT_OBJECT_MAPPER.readTree(streamsOutput).toString();
                minimalBufferStreamsOutput = ParseAndMaskUtil.DEFAULT_OBJECT_MAPPER.readTree(minimalBufferStreamsOutput).toString();
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed for input: " + input, e);
            }
        }
        if (expectedOutput != null) {
            Assertions.assertEquals(expectedOutput, bytesOutput, "Failed for input: " + input);
        }
        Assertions.assertEquals(bytesOutput, streamsOutput, "Streaming failed for input: " + input);
        Assertions.assertEquals(streamsOutput, minimalBufferStreamsOutput, "Minimal buffer streaming failed for input: " + input);
    }

    public static void assertJsonMaskerApiEquivalence(JsonMasker jsonMasker, String input) {
        assertJsonMaskerApiEquivalence(jsonMasker, input, null, false);
    }

    public static void assertJsonMaskerApiEquivalence(JsonMasker jsonMasker, String input, String expectedOutput) {
        assertJsonMaskerApiEquivalence(jsonMasker, input, expectedOutput, false);
    }

    private static String getStreamingModeOutput(JsonMasker jsonMasker, String input) {
        ByteArrayOutputStream streamsOutput = new ByteArrayOutputStream();
        Assertions.assertDoesNotThrow(() -> jsonMasker.mask(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), streamsOutput));
        return streamsOutput.toString(StandardCharsets.UTF_8);
    }
}
