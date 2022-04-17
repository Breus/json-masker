package masker.json;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import randomgen.json.RandomJsonGenerator;
import randomgen.json.RandomJsonGeneratorConfig;

public class FuzzingTest {

    @ParameterizedTest
    @ValueSource(ints = {10000}) // number of tests
    public void fuzzTestSingleTarget(int amountOfTests) {
        for (int i = 0; i < amountOfTests; i++) {
            JsonMasker keyContainsMasker = JsonMasker.getMasker("targetKey1", JsonMaskingConfig.custom().multiTargetAlgorithm(JsonMultiTargetAlgorithm.KEYS_CONTAIN).build());
            JsonMasker singleTargetMasker = JsonMasker.getMasker("targetKey1", JsonMaskingConfig.custom().multiTargetAlgorithm(JsonMultiTargetAlgorithm.SINGLE_TARGET_LOOP).build());
            RandomJsonGenerator randomJsonGenerator = new RandomJsonGenerator(RandomJsonGeneratorConfig.builder().createConfig());
            JsonNode randomJsonNode = randomJsonGenerator.createRandomJsonNode();
            String randomJsonNodeString = randomJsonNode.toString();
            String keyContainsOutput = keyContainsMasker.mask(randomJsonNodeString);
            String singleTargetOutput = singleTargetMasker.mask(randomJsonNodeString);
            Assertions.assertEquals(keyContainsOutput, singleTargetOutput);
        }
    }
}
