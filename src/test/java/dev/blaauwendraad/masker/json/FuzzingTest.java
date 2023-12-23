package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.databind.JsonNode;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import randomgen.json.RandomJsonGenerator;
import randomgen.json.RandomJsonGeneratorConfig;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Stream;

final class FuzzingTest {
    private static final Set<String> DEFAULT_TARGET_KEYS = Set.of("targetKey1", "targetKey2", "targetKey3");
    private static final int SECONDS_FOR_EACH_TEST_TO_RUN = 2;

    @ParameterizedTest
    @MethodSource("jsonMaskingConfigs")
    void fuzzingAgainstParseAndMaskUsingJackson(JsonMaskingConfig jsonMaskingConfig) {
        Instant startTime = Instant.now();
        int randomTestExecuted = 0;
        RandomJsonGenerator randomJsonGenerator = new RandomJsonGenerator(RandomJsonGeneratorConfig.builder()
                                                                                  .setTargetKeys(jsonMaskingConfig.getTargetKeys())
                                                                                  .createConfig());
        JsonMasker masker = JsonMasker.getMasker(jsonMaskingConfig);
        while (Duration.between(startTime, Instant.now()).getSeconds() < SECONDS_FOR_EACH_TEST_TO_RUN) {
            JsonNode randomJsonNode = randomJsonGenerator.createRandomJsonNode();
            String randomJsonNodeString = randomJsonNode.toPrettyString();
            String keyContainsOutput = masker.mask(randomJsonNodeString);
            String jacksonMaskingOutput = ParseAndMaskUtil.mask(randomJsonNode, jsonMaskingConfig).toPrettyString();
            Assertions.assertEquals(jacksonMaskingOutput,
                                    keyContainsOutput,
                                    "Failed for input: " + randomJsonNodeString
            );
            randomTestExecuted++;
        }
        System.out.printf("Executed %d randomly generated test scenarios in %d seconds%n",
                          randomTestExecuted,
                          SECONDS_FOR_EACH_TEST_TO_RUN
        );
    }

    @Nonnull
    private static Stream<JsonMaskingConfig> jsonMaskingConfigs() {
        return Stream.of(JsonMaskingConfig.getDefault(DEFAULT_TARGET_KEYS),
                         JsonMaskingConfig.custom(DEFAULT_TARGET_KEYS, JsonMaskingConfig.TargetKeyMode.ALLOW).build()
        );
    }
}
