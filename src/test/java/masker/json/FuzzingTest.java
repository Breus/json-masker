package masker.json;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import randomgen.json.RandomJsonGenerator;
import randomgen.json.RandomJsonGeneratorConfig;

import java.util.Set;

class FuzzingTest {

    @ParameterizedTest
    @ValueSource(ints = {10000})
        // number of tests
    void fuzzTestNoFailuresKeyContainsAlgorithm(int amountOfTests) {
        for (int i = 0; i < amountOfTests; i++) {
            Set<String> targetKeys = Set.of("targetKey1", "targetKey2");
            JsonMasker keyContainsMasker = JsonMasker.getMasker(targetKeys,
                                                                JsonMaskingConfig.custom()
                                                                        .multiTargetAlgorithm(JsonMultiTargetAlgorithm.KEYS_CONTAIN)
                                                                        .build());
            RandomJsonGenerator randomJsonGenerator =
                    new RandomJsonGenerator(RandomJsonGeneratorConfig.builder().createConfig());
            JsonNode randomJsonNode = randomJsonGenerator.createRandomJsonNode();
            Assertions.assertDoesNotThrow(() -> keyContainsMasker.mask(randomJsonNode.toPrettyString()),
                                          randomJsonNode.toPrettyString());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {100000})
        // number of tests
    void fuzzTestNoFailuresSingleTargetLoopAlgorithm(int amountOfTests) {
        for (int i = 0; i < amountOfTests; i++) {
            Set<String> targetKeys = Set.of("targetKey1", "targetKey2");
            JsonMasker singleTargetMasker = JsonMasker.getMasker(targetKeys,
                                                                 JsonMaskingConfig.custom()
                                                                         .multiTargetAlgorithm(JsonMultiTargetAlgorithm.SINGLE_TARGET_LOOP)
                                                                         .build());
            RandomJsonGenerator randomJsonGenerator =
                    new RandomJsonGenerator(RandomJsonGeneratorConfig.builder().createConfig());
            JsonNode randomJsonNode = randomJsonGenerator.createRandomJsonNode();
            Assertions.assertDoesNotThrow(() -> singleTargetMasker.mask(randomJsonNode.toPrettyString()),
                                          randomJsonNode.toPrettyString());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {100000})
        // number of tests
    void fuzzTestTwoTargets(int amountOfTests) {
        for (int i = 0; i < amountOfTests; i++) {
            Set<String> targetKeys = Set.of("targetKey1", "targetKey2");
            JsonMasker keyContainsMasker = JsonMasker.getMasker(targetKeys,
                                                                JsonMaskingConfig.custom()
                                                                        .multiTargetAlgorithm(JsonMultiTargetAlgorithm.KEYS_CONTAIN)
                                                                        .build());
            JsonMasker singleTargetMasker = JsonMasker.getMasker(targetKeys,
                                                                 JsonMaskingConfig.custom()
                                                                         .multiTargetAlgorithm(JsonMultiTargetAlgorithm.SINGLE_TARGET_LOOP)
                                                                         .build());
            RandomJsonGenerator randomJsonGenerator =
                    new RandomJsonGenerator(RandomJsonGeneratorConfig.builder().createConfig());
            JsonNode randomJsonNode = randomJsonGenerator.createRandomJsonNode();
            String randomJsonNodeString = randomJsonNode.toPrettyString();
            String keyContainsOutput = keyContainsMasker.mask(randomJsonNodeString);
            String singleTargetOutput = singleTargetMasker.mask(randomJsonNodeString);
            String jacksonMaskingOutput = ParseAndMaskUtil.mask(randomJsonNode, targetKeys).toPrettyString();
            Assertions.assertEquals(keyContainsOutput, singleTargetOutput, "Failed for input: " + randomJsonNodeString);
            Assertions.assertEquals(jacksonMaskingOutput,
                                    keyContainsOutput,
                                    "Failed for input: " + randomJsonNodeString);
        }
    }
}
