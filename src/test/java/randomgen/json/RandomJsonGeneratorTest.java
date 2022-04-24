package randomgen.json;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class RandomJsonGeneratorTest {
    @ParameterizedTest
    @ValueSource(ints = 100)
    void testRandomGenerator(int numberOfTests) {
        for (int i = 0; i < numberOfTests; i++) {
            RandomJsonGenerator randomJsonGenerator = new RandomJsonGenerator(RandomJsonGeneratorConfig.builder().createConfig());
            JsonNode randomJsonNode = randomJsonGenerator.createRandomJsonNode();
            System.out.println(randomJsonNode.toPrettyString());
        }
    }
}
