package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.openjdk.jmh.annotations.*;
import randomgen.json.JsonPathTestUtils;

import java.nio.charset.StandardCharsets;
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
        @Param({ "1kb", "128kb", "2mb" })
        String jsonSize;
        @Param({ "ascii (no quote)", "ascii", "unicode" })
        String characters;
        @Param({ "0.01", "0.1" })
        double maskedKeyProbability;
        @Param({ "false", "true" })
        boolean jsonPath;

        private String jsonString;
        private byte[] jsonBytes;
        private JsonMasker jsonMasker;

        @Setup
        public synchronized void setup() {
            Set<String> targetKeys = BenchmarkUtils.getTargetKeys(20);
            jsonString = BenchmarkUtils.randomJson(targetKeys, jsonSize, characters, maskedKeyProbability);
            jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);

            JsonMaskingConfig.Builder builder = JsonMaskingConfig.builder();
            if (jsonPath) {
                builder.maskJsonPaths(JsonPathTestUtils.transformToJsonPathKeys(targetKeys, jsonString));
            } else {
                builder.maskKeys(targetKeys);
            }
            jsonMasker = JsonMasker.getMasker(builder.build());
        }
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
