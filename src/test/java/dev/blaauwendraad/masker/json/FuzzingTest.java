package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.databind.JsonNode;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.path.JsonPath;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import randomgen.json.RandomJsonGenerator;
import randomgen.json.RandomJsonGeneratorConfig;

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
    public static final int TOTAL_TEST_DURATION_SECONDS = 30;
    private static final long MILLISECONDS_FOR_EACH_TEST_TO_RUN =
            Duration.ofSeconds(TOTAL_TEST_DURATION_SECONDS).toMillis() / (int) jsonMaskingConfigs().count();

    @ParameterizedTest
    @MethodSource("jsonMaskingConfigs")
    void fuzzingAgainstParseAndMaskUsingJackson(JsonMaskingConfig jsonMaskingConfig) {
        Instant startTime = Instant.now();
        int randomTestExecuted = 0;
        Set<String> allKeys = new HashSet<>(jsonMaskingConfig.getTargetKeys());
        allKeys.addAll(jsonMaskingConfig.getTargetJsonPaths().stream().map(JsonPath::getLastSegment).collect(Collectors.toSet()));
        RandomJsonGenerator randomJsonGenerator = new RandomJsonGenerator(RandomJsonGeneratorConfig.builder()
                .setTargetKeys(allKeys)
                .createConfig());
        JsonMasker masker = JsonMasker.getMasker(jsonMaskingConfig);
        while (Duration.between(startTime, Instant.now()).toMillis() < MILLISECONDS_FOR_EACH_TEST_TO_RUN) {
            JsonNode randomJsonNode = randomJsonGenerator.createRandomJsonNode();
            String randomJsonNodeString = randomJsonNode.toPrettyString();
            String keyContainsOutput;
            try {
                keyContainsOutput = masker.mask(randomJsonNodeString);
            } catch (Exception e) {
                throw new IllegalStateException("Failed for input: " + randomJsonNodeString, e);
            }
            String jacksonMaskingOutput = ParseAndMaskUtil.mask(randomJsonNode, jsonMaskingConfig).toPrettyString();
            assertThat(keyContainsOutput).as("Failed for input: " + randomJsonNodeString).isEqualTo(jacksonMaskingOutput);
            randomTestExecuted++;
        }
        System.out.printf(
                "Executed %d randomly generated test scenarios in %d milliseconds%n",
                randomTestExecuted,
                MILLISECONDS_FOR_EACH_TEST_TO_RUN
        );
    }

    @Nonnull
    private static Stream<JsonMaskingConfig> jsonMaskingConfigs() {
        return Stream.of(
                JsonMaskingConfig.builder().maskKeys(DEFAULT_TARGET_KEYS).build(),
                JsonMaskingConfig.builder().maskKeys(DEFAULT_TARGET_KEYS)
                        .maskStringCharactersWith("*")
                        .maskNumberDigitsWith(1)
                        .build(),
                JsonMaskingConfig.builder().maskKeys(DEFAULT_TARGET_KEYS)
                        .caseSensitiveTargetKeys()
                        .build(),
                JsonMaskingConfig.builder().maskKeys(DEFAULT_TARGET_KEYS)
                        .maskStringsWith("***")
                        .disableNumberMasking()
                        .disableBooleanMasking()
                        .build(),
                JsonMaskingConfig.builder().maskKeys(DEFAULT_TARGET_KEYS)
                        .maskStringsWith("")
                        .disableNumberMasking()
                        .disableBooleanMasking()
                        .build(),
                JsonMaskingConfig.builder().maskKeys(DEFAULT_TARGET_KEYS)
                        .maskStringsWith("*")
                        .maskNumbersWith(1)
                        .build(),
                JsonMaskingConfig.builder().maskKeys(DEFAULT_TARGET_KEYS)
                        .maskStringsWith("***")
                        .maskNumbersWith(111)
                        .build(),
                JsonMaskingConfig.builder().allowKeys(DEFAULT_TARGET_KEYS).build(),
                JsonMaskingConfig.builder().allowKeys(DEFAULT_TARGET_KEYS)
                        .caseSensitiveTargetKeys()
                        .build(),
                JsonMaskingConfig.builder().allowKeys(DEFAULT_TARGET_KEYS)
                        .maskNumberDigitsWith(2)
                        .build(),
                JsonMaskingConfig.builder().allowKeys(DEFAULT_TARGET_KEYS)
                        .maskStringsWith("")
                        .disableNumberMasking()
                        .disableBooleanMasking()
                        .build(),
                JsonMaskingConfig.builder().allowKeys(DEFAULT_TARGET_KEYS)
                        .maskStringsWith("****")
                        .maskNumbersWith(11111111)
                        .build(),
                JsonMaskingConfig.builder().allowKeys(DEFAULT_TARGET_KEYS)
                        .maskStringsWith("****")
                        .build(),
                JsonMaskingConfig.builder().maskJsonPaths(DEFAULT_JSON_PATH_KEYS).build(),
                JsonMaskingConfig.builder().maskJsonPaths(DEFAULT_JSON_PATH_KEYS)
                        .maskStringCharactersWith("*")
                        .maskNumberDigitsWith(1)
                        .build(),
                JsonMaskingConfig.builder().maskJsonPaths(DEFAULT_JSON_PATH_KEYS)
                        .caseSensitiveTargetKeys()
                        .build(),
                JsonMaskingConfig.builder().maskJsonPaths(DEFAULT_JSON_PATH_KEYS)
                        .maskStringsWith("***")
                        .disableNumberMasking()
                        .disableBooleanMasking()
                        .build(),
                JsonMaskingConfig.builder().maskJsonPaths(DEFAULT_JSON_PATH_KEYS)
                        .maskStringsWith("")
                        .disableNumberMasking()
                        .disableBooleanMasking()
                        .build(),
                JsonMaskingConfig.builder().maskJsonPaths(DEFAULT_JSON_PATH_KEYS)
                        .maskStringsWith("*")
                        .maskNumbersWith(1)
                        .build(),
                JsonMaskingConfig.builder().maskJsonPaths(DEFAULT_JSON_PATH_KEYS)
                        .maskStringsWith("***")
                        .maskNumbersWith(111)
                        .build(),
                JsonMaskingConfig.builder().allowJsonPaths(DEFAULT_JSON_PATH_KEYS).build(),
                JsonMaskingConfig.builder().allowJsonPaths(DEFAULT_JSON_PATH_KEYS)
                        .caseSensitiveTargetKeys()
                        .build(),
                JsonMaskingConfig.builder().allowJsonPaths(DEFAULT_JSON_PATH_KEYS)
                        .maskNumberDigitsWith(2)
                        .build(),
                JsonMaskingConfig.builder().allowJsonPaths(DEFAULT_JSON_PATH_KEYS)
                        .maskStringsWith("")
                        .disableNumberMasking()
                        .disableBooleanMasking()
                        .build(),
                JsonMaskingConfig.builder().allowJsonPaths(DEFAULT_JSON_PATH_KEYS)
                        .maskStringsWith("****")
                        .maskNumbersWith(11111111)
                        .build(),
                JsonMaskingConfig.builder().allowJsonPaths(DEFAULT_JSON_PATH_KEYS)
                        .maskStringsWith("****")
                        .build()
        );
    }
}
