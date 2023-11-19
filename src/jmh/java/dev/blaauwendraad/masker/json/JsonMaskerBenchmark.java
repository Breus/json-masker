package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskerAlgorithmType;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.openjdk.jmh.annotations.*;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 1, time = 3)
@Fork(value = 1)
@Measurement(iterations = 1, time = 3)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
public class JsonMaskerBenchmark {

    @org.openjdk.jmh.annotations.State(Scope.Thread)
    public static class State {

        @Param({"200b", "4kb", "128kb", "2mb"})
        String jsonSize;

        @Param({"1", "100"})
        int numberOfKeys;

        @Param({"-1", "8"})
        int obfuscationLength;
        private String jsonString;
        private byte[] jsonBytes;
        private JsonMasker jsonMasker;

        @Setup
        public synchronized void setup() {
            Set<String> keysToBeMasked = getTargetKeys(numberOfKeys);

            jsonString = ParseAndMaskUtil.readJsonFromFileAsString("json-%s.json".formatted(jsonSize), this.getClass());
            jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);

            jsonMasker = JsonMasker.getMasker(
                    JsonMaskingConfig.custom(keysToBeMasked, JsonMaskingConfig.TargetKeyMode.MASK)
                            .obfuscationLength(obfuscationLength)
                            .algorithmTypeOverride(JsonMaskerAlgorithmType.KEYS_CONTAIN)
                            .build()
            );
        }

        private Set<String> getTargetKeys(int numberOfKeys) {
            Set<String> targetKeys = new HashSet<>();
            for (int i = 0; i < numberOfKeys; i++) {
                targetKeys.add("someSecret" + i);
            }
            return targetKeys;
        }
    }

    @Benchmark
    public String maskJsonString(State state) {
        return state.jsonMasker.mask(state.jsonString);
    }

    @Benchmark
    public byte[] maskJsonBytes(State state) {
        return state.jsonMasker.mask(state.jsonBytes);
    }
}
