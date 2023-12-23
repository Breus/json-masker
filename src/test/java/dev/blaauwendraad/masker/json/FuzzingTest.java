package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.databind.JsonNode;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import randomgen.json.RandomJsonGenerator;
import randomgen.json.RandomJsonGeneratorConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

final class FuzzingTest {
    private static final int SECONDS_FOR_EACH_TEST_TO_RUN = 10;

    @ParameterizedTest
    @ValueSource(ints = { SECONDS_FOR_EACH_TEST_TO_RUN })
        // duration in seconds the tests runs for
    void defaultMaskingConfiguration(int secondsToRunTest) {
        Instant startTime = Instant.now();
        int randomTestExecuted = 0;
        while (Duration.between(startTime, Instant.now()).getSeconds() < secondsToRunTest) {
            Set<String> targetKeys = Set.of("targetKey1", "targetKey2");
            JsonMasker keyContainsMasker = new KeyContainsMasker(JsonMaskingConfig.getDefault(
                    targetKeys
            ));
            RandomJsonGenerator randomJsonGenerator =
                    new RandomJsonGenerator(RandomJsonGeneratorConfig.builder().createConfig());
            JsonNode randomJsonNode = randomJsonGenerator.createRandomJsonNode();
            String randomJsonNodeString = randomJsonNode.toPrettyString();
            String keyContainsOutput = keyContainsMasker.mask(randomJsonNodeString);
            String jacksonMaskingOutput = ParseAndMaskUtil.mask(
                    randomJsonNode,
                    targetKeys,
                    JsonMaskingConfig.TargetKeyMode.MASK
            ).toPrettyString();
            Assertions.assertEquals(
                    jacksonMaskingOutput,
                    keyContainsOutput,
                    "Failed for input: " + randomJsonNodeString
            );
            randomTestExecuted++;
        }
        System.out.printf(
                "Executed %d randomly generated test scenarios in %d seconds%n",
                randomTestExecuted,
                secondsToRunTest
        );
    }
}
