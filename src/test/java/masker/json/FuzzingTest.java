package masker.json;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import randomgen.json.RandomJsonGenerator;
import randomgen.json.RandomJsonGeneratorConfig;

import java.util.Set;

public class FuzzingTest {

    @ParameterizedTest
    @ValueSource(ints = {1}) // number of tests
    public void fuzzTestSingleTarget(int amountOfTests) {
        for (int i = 0; i < amountOfTests; i++) {
            Set<String> targetKeys = Set.of("targetKey1", "targetKey2");
            JsonMasker keyContainsMasker = JsonMasker.getMasker(targetKeys, JsonMaskingConfig.custom().multiTargetAlgorithm(JsonMultiTargetAlgorithm.KEYS_CONTAIN).build());
            JsonMasker singleTargetMasker = JsonMasker.getMasker(targetKeys, JsonMaskingConfig.custom().multiTargetAlgorithm(JsonMultiTargetAlgorithm.SINGLE_TARGET_LOOP).build());
            RandomJsonGenerator randomJsonGenerator = new RandomJsonGenerator(RandomJsonGeneratorConfig.builder().createConfig());
            JsonNode randomJsonNode = randomJsonGenerator.createRandomJsonNode();
            String randomJsonNodeString = randomJsonNode.toPrettyString();
            System.out.println(randomJsonNodeString);
            String keyContainsOutput = keyContainsMasker.mask(randomJsonNodeString);
            String singleTargetOutput = singleTargetMasker.mask(randomJsonNodeString);
            String jacksonMaskingOutput = ParseAndMaskUtil.mask(randomJsonNode, targetKeys).toString();
            Assertions.assertEquals(keyContainsOutput, singleTargetOutput);
            Assertions.assertEquals(jacksonMaskingOutput, singleTargetOutput);
        }
    }
}
