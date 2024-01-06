package randomgen.json;

import com.fasterxml.jackson.databind.JsonNode;
import dev.blaauwendraad.masker.json.JsonMasker;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;
import static randomgen.json.JsonStringCharacters.getPrintableAsciiCharacters;

public class RandomJsonGeneratorTest {
    @ParameterizedTest
    @ValueSource(ints = 100)
    void testRandomGenerator(int numberOfTests) {
        for (int i = 0; i < numberOfTests; i++) {
            RandomJsonGenerator randomJsonGenerator = new RandomJsonGenerator(
                    RandomJsonGeneratorConfig.builder().createConfig()
            );
            JsonNode randomJsonNode = randomJsonGenerator.createRandomJsonNode();
            System.out.println(randomJsonNode.toPrettyString());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = 100)
    void testGeneratesJsonForAGivenSize(int numberOfTests) {
        for (int i = 0; i < numberOfTests; i++) {
            // let's start with at least 5kb json size, on smaller sizes (e.g. 1kb)
            // we risk having more than 1% difference due to requirement to return a valid json
            int targetJsonSizeBytes = (i + 5) * 1024;
            RandomJsonGenerator randomJsonGenerator = new RandomJsonGenerator(
                    RandomJsonGeneratorConfig.builder()
                            .setTargetJsonSizeBytes(targetJsonSizeBytes)
                            .createConfig()
            );

            JsonNode randomJsonNode = randomJsonGenerator.createRandomJsonNode();

            int actualSizeBytes = randomJsonNode.toString().getBytes(StandardCharsets.UTF_8).length;

            double allowedDifference = targetJsonSizeBytes * 0.01;
            assertThat(actualSizeBytes)
                    .as(() -> "Expected json to be of size " + targetJsonSizeBytes + " (±1%), got: " + actualSizeBytes)
                    .isCloseTo(targetJsonSizeBytes, withinPercentage(allowedDifference));
        }
    }

    @Test
    void testGenerate1MbJson() {
        int targetJsonSizeBytes = 1024 * 1024;
        RandomJsonGenerator randomJsonGenerator = new RandomJsonGenerator(
                RandomJsonGeneratorConfig.builder()
                        .setTargetJsonSizeBytes(targetJsonSizeBytes)
                        .createConfig()
        );

        JsonNode randomJsonNode = randomJsonGenerator.createRandomJsonNode();

        int actualSizeBytes = randomJsonNode.toString().getBytes(StandardCharsets.UTF_8).length;

        double allowedDifference = targetJsonSizeBytes * 0.001;
        assertThat(actualSizeBytes)
                .as(() -> "Expected json to be of size " + targetJsonSizeBytes + " (±0.1%), got: " + actualSizeBytes)
                .isCloseTo(targetJsonSizeBytes, withinPercentage(allowedDifference));
    }

    @Test
    void testGenerate10MbJson() {
        int targetJsonSizeBytes = 10 * 1024 * 1024;
        RandomJsonGenerator randomJsonGenerator = new RandomJsonGenerator(
                RandomJsonGeneratorConfig.builder()
                        .setTargetJsonSizeBytes(targetJsonSizeBytes)
                        .createConfig()
        );

        JsonNode randomJsonNode = randomJsonGenerator.createRandomJsonNode();

        int actualSizeBytes = randomJsonNode.toString().getBytes(StandardCharsets.UTF_8).length;

        double allowedDifference = targetJsonSizeBytes * 0.001;
        assertThat(actualSizeBytes).as(() -> "Expected json to be of size " + targetJsonSizeBytes + " (±0.1%), got: "
                + actualSizeBytes).isCloseTo(targetJsonSizeBytes, withinPercentage(allowedDifference));
    }

    @Test
    @Disabled
    void profileMasker() {
        int targetJsonSizeBytes = 10 * 1024 * 1024;
        RandomJsonGenerator randomJsonGenerator = new RandomJsonGenerator(
                RandomJsonGeneratorConfig.builder()
                        .setAllowedCharacters(
                                getPrintableAsciiCharacters().stream().filter(c -> c != '"').collect(Collectors.toSet())
                        )
                        .setTargetJsonSizeBytes(targetJsonSizeBytes)
                        .createConfig()
        );

        JsonNode randomJsonNode = randomJsonGenerator.createRandomJsonNode();

        var json = randomJsonNode.toString().getBytes(StandardCharsets.UTF_8);

        JsonMasker jsonMasker = JsonMasker.getMasker(Set.of("targetKey1", "targetKey2", "targetKey3", "targetKey4"));

        int size = 0;
        for (int i = 0; i < 500; i++) {
            size += jsonMasker.mask(json).length;
        }

        System.out.println(size);
    }

    @Test
    void shouldReturnIdenticalJsonsForTheSameSeed() {
        RandomJsonGenerator randomJsonGenerator = new RandomJsonGenerator(
                RandomJsonGeneratorConfig.builder()
                        .setTargetJsonSizeBytes(10 * 1024)
                        .setRandomSeed(1285756302517652226L)
                        .createConfig()
        );

        String json1 = randomJsonGenerator.createRandomJsonNode().toString();
        String json2 = randomJsonGenerator.createRandomJsonNode().toString();

        assertThat(json2).isEqualTo(json1);
    }

    @Test
    void shouldReturnDifferentJsonsIfSeedIsNotSet() {
        RandomJsonGenerator randomJsonGenerator = new RandomJsonGenerator(
                RandomJsonGeneratorConfig.builder()
                        .setTargetJsonSizeBytes(10 * 1024)
                        .createConfig()
        );

        String json1 = randomJsonGenerator.createRandomJsonNode().toString();
        String json2 = randomJsonGenerator.createRandomJsonNode().toString();

        assertThat(json2).isNotEqualTo(json1);
    }

    @Test
    void shouldReturnDifferentJsonsForDifferentParameters() {
        RandomJsonGenerator randomJsonGenerator1 = new RandomJsonGenerator(
                RandomJsonGeneratorConfig.builder()
                        .setTargetJsonSizeBytes(10 * 1024)
                        .setRandomSeed(1285756302517652226L)
                        .createConfig()
        );
        RandomJsonGenerator randomJsonGenerator2 = new RandomJsonGenerator(
                RandomJsonGeneratorConfig.builder()
                        .setTargetJsonSizeBytes(10 * 1024 + 100)
                        .setRandomSeed(1285756302517652226L)
                        .createConfig()
        );

        String json1 = randomJsonGenerator1.createRandomJsonNode().toString();
        String json2 = randomJsonGenerator2.createRandomJsonNode().toString();

        assertThat(json2).isNotEqualTo(json1);
    }
}
