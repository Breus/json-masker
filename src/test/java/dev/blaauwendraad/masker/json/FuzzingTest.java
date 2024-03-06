package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.path.JsonPath;
import dev.blaauwendraad.masker.json.util.FuzzingDurationUtil;
import dev.blaauwendraad.masker.json.util.JsonFormatter;
import dev.blaauwendraad.masker.randomgen.RandomJsonGenerator;
import dev.blaauwendraad.masker.randomgen.RandomJsonGeneratorConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

final class FuzzingTest {
    private static final Set<String> DEFAULT_TARGET_KEYS = Set.of("targetKey1", "targetKey2", "targetKey3");
    private static final Set<String> DEFAULT_JSON_PATH_KEYS = Set.of("$.targetKey1", "$.targetKey2", "$.targetKey3");

    @ParameterizedTest
    @MethodSource("jsonMaskingConfigs")
    void fuzzingAgainstParseAndMaskUsingJackson(JsonMaskingConfig jsonMaskingConfig) throws JsonProcessingException {
        long timeLimit = FuzzingDurationUtil.determineTestTimeLimit(jsonMaskingConfigs().count());
        Instant startTime = Instant.now();
        int randomTestExecuted = 0;
        Set<String> allKeys = new HashSet<>(jsonMaskingConfig.getTargetKeys());
        allKeys.addAll(
                jsonMaskingConfig.getTargetJsonPaths().stream()
                        .map(JsonPath::getQueryArgument)
                        .collect(Collectors.toSet()));
        RandomJsonGenerator randomJsonGenerator =
                new RandomJsonGenerator(RandomJsonGeneratorConfig.builder().setTargetKeys(allKeys).createConfig());
        JsonMasker masker = JsonMasker.getMasker(jsonMaskingConfig);
        while (Duration.between(startTime, Instant.now()).toMillis() < timeLimit) {
            JsonNode randomJsonNode = randomJsonGenerator.createRandomJsonNode();
            for (JsonFormatter formatter : JsonFormatter.values()) {
                String randomJsonString = formatter.format(randomJsonNode);
                String jacksonMaskingOutput = ParseAndMaskUtil.mask(randomJsonString, jsonMaskingConfig).toString();
                try {
                    String keyContainsOutput = masker.mask(randomJsonString);
                    // parsing again by jackson to get canonical representation
                    assertThat(ParseAndMaskUtil.DEFAULT_OBJECT_MAPPER.readTree(keyContainsOutput))
                            .as("Failed for input: " + randomJsonString)
                            .isEqualTo(ParseAndMaskUtil.DEFAULT_OBJECT_MAPPER.readTree(jacksonMaskingOutput));
                } catch (Exception e) {
                    throw new IllegalStateException("Failed for input: " + randomJsonString, e);
                }
                randomTestExecuted++;
            }
        }
        System.out.printf(
                "Executed %d randomly generated test scenarios in %d milliseconds%n",
                randomTestExecuted, timeLimit);
    }

    @Nonnull
    private static Stream<JsonMaskingConfig> jsonMaskingConfigs() {
        return Stream.of(
                JsonMaskingConfig.builder().maskKeys(DEFAULT_TARGET_KEYS).build(),
                JsonMaskingConfig.builder()
                        .maskKeys(DEFAULT_TARGET_KEYS)
                        .maskStringCharactersWith("*")
                        .maskNumberDigitsWith(1)
                        .build(),
                JsonMaskingConfig.builder().maskKeys(DEFAULT_TARGET_KEYS).caseSensitiveTargetKeys().build(),
                JsonMaskingConfig.builder()
                        .maskKeys(DEFAULT_TARGET_KEYS)
                        .maskStringsWith("***")
                        .disableNumberMasking()
                        .disableBooleanMasking()
                        .build(),
                JsonMaskingConfig.builder()
                        .maskKeys(DEFAULT_TARGET_KEYS)
                        .maskStringsWith("")
                        .disableNumberMasking()
                        .disableBooleanMasking()
                        .build(),
                JsonMaskingConfig.builder()
                        .maskKeys(DEFAULT_TARGET_KEYS)
                        .maskStringsWith("*")
                        .maskNumbersWith(1)
                        .build(),
                JsonMaskingConfig.builder()
                        .maskKeys(DEFAULT_TARGET_KEYS)
                        .maskStringsWith("***")
                        .maskNumbersWith(111)
                        .build(),
                JsonMaskingConfig.builder().allowKeys(DEFAULT_TARGET_KEYS).build(),
                JsonMaskingConfig.builder().allowKeys(DEFAULT_TARGET_KEYS).caseSensitiveTargetKeys().build(),
                JsonMaskingConfig.builder().allowKeys(DEFAULT_TARGET_KEYS).maskNumberDigitsWith(2).build(),
                JsonMaskingConfig.builder()
                        .allowKeys(DEFAULT_TARGET_KEYS)
                        .maskStringsWith("")
                        .disableNumberMasking()
                        .disableBooleanMasking()
                        .build(),
                JsonMaskingConfig.builder()
                        .allowKeys(DEFAULT_TARGET_KEYS)
                        .maskStringsWith("****")
                        .maskNumbersWith(11111111)
                        .build(),
                JsonMaskingConfig.builder().allowKeys(DEFAULT_TARGET_KEYS).maskStringsWith("****").build(),
                JsonMaskingConfig.builder().maskJsonPaths(DEFAULT_JSON_PATH_KEYS).build(),
                JsonMaskingConfig.builder()
                        .maskJsonPaths(DEFAULT_JSON_PATH_KEYS)
                        .maskStringCharactersWith("*")
                        .maskNumberDigitsWith(1)
                        .build(),
                JsonMaskingConfig.builder().maskJsonPaths(DEFAULT_JSON_PATH_KEYS).caseSensitiveTargetKeys().build(),
                JsonMaskingConfig.builder()
                        .maskJsonPaths(DEFAULT_JSON_PATH_KEYS)
                        .maskStringsWith("***")
                        .disableNumberMasking()
                        .disableBooleanMasking()
                        .build(),
                JsonMaskingConfig.builder()
                        .maskJsonPaths(DEFAULT_JSON_PATH_KEYS)
                        .maskStringsWith("")
                        .disableNumberMasking()
                        .disableBooleanMasking()
                        .build(),
                JsonMaskingConfig.builder()
                        .maskJsonPaths(DEFAULT_JSON_PATH_KEYS)
                        .maskStringsWith("*")
                        .maskNumbersWith(1)
                        .build(),
                JsonMaskingConfig.builder()
                        .maskJsonPaths(DEFAULT_JSON_PATH_KEYS)
                        .maskStringsWith("***")
                        .maskNumbersWith(111)
                        .build(),
                JsonMaskingConfig.builder().allowJsonPaths(DEFAULT_JSON_PATH_KEYS).build(),
                JsonMaskingConfig.builder().allowJsonPaths(DEFAULT_JSON_PATH_KEYS).caseSensitiveTargetKeys().build(),
                JsonMaskingConfig.builder().allowJsonPaths(DEFAULT_JSON_PATH_KEYS).maskNumberDigitsWith(2).build(),
                JsonMaskingConfig.builder()
                        .allowJsonPaths(DEFAULT_JSON_PATH_KEYS)
                        .maskStringsWith("")
                        .disableNumberMasking()
                        .disableBooleanMasking()
                        .build(),
                JsonMaskingConfig.builder()
                        .allowJsonPaths(DEFAULT_JSON_PATH_KEYS)
                        .maskStringsWith("****")
                        .maskNumbersWith(11111111)
                        .build(),
                JsonMaskingConfig.builder().allowJsonPaths(DEFAULT_JSON_PATH_KEYS).maskStringsWith("****").build());
    }
}
