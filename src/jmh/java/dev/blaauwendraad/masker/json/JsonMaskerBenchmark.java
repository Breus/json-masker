package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Warmup;
import randomgen.json.RandomJsonGenerator;
import randomgen.json.RandomJsonGeneratorConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static randomgen.json.JsonStringCharacters.getPrintableAsciiCharacters;

@Warmup(iterations = 1, time = 3)
@Fork(value = 1)
@Measurement(iterations = 1, time = 3)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
public class JsonMaskerBenchmark {

    @org.openjdk.jmh.annotations.State(Scope.Thread)
    public static class State {
        @Param({ "1kb", "128kb", "2mb" })
        String jsonSize;
        @Param({ "0.01", "0.1" })
        double maskedKeyProbability;
        @Param({ "none", "8" })
        String obfuscationLength;
        private String jsonString;
        private byte[] jsonBytes;
        private JsonMasker jsonMasker;
        private ObjectMapper objectMapper;

        @Setup
        public synchronized void setup() {
            Set<String> keysToBeMasked = getTargetKeys();

            RandomJsonGeneratorConfig config = RandomJsonGeneratorConfig.builder()
                    .setAllowedCharacters(
                            getPrintableAsciiCharacters().stream()
                                    .filter(c -> c != '"')
                                    .collect(Collectors.toSet())
                    )
                    .setTargetKeys(keysToBeMasked)
                    .setTargetKeyPercentage(maskedKeyProbability)
                    .setTargetJsonSizeBytes(BenchmarkUtils.parseSize(jsonSize))
                    .createConfig();

            jsonString = new RandomJsonGenerator(config).createRandomJsonNode().toString();
            jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);

            jsonMasker = JsonMasker.getMasker(
                    JsonMaskingConfig.custom(keysToBeMasked, JsonMaskingConfig.TargetKeyMode.MASK)
                            .obfuscationLength(Objects.equals(obfuscationLength, "none")
                                                       ? -1
                                                       : Integer.parseInt(obfuscationLength))
                            .build()
            );
            objectMapper = new ObjectMapper();
        }

        private Set<String> getTargetKeys() {
            Set<String> targetKeys = new HashSet<>();
            for (int i = 0; i < 20; i++) {
                targetKeys.add("someSecret" + i);
            }
            return targetKeys;
        }
    }

    @Benchmark
    public int baselineCountBytes(State state) {
        int sum = 0;
        for (int i = 0; i < state.jsonBytes.length; i++) {
            sum += state.jsonBytes[i];
        }
        return sum;
    }

    @Benchmark
    public String jacksonString(State state) throws IOException {
        return ParseAndMaskUtil.mask(
                state.jsonString,
                state.getTargetKeys(),
                JsonMaskingConfig.TargetKeyMode.MASK,
                state.objectMapper
        ).toString();
    }

    @Benchmark
    public String jsonMaskerString(State state) {
        return state.jsonMasker.mask(state.jsonString);
    }

    @Benchmark
    public byte[] jsonMaskerBytes(State state) {
        return state.jsonMasker.mask(state.jsonBytes);
    }
}
