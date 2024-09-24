package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.databind.JsonNode;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.jspecify.annotations.NullUnmarked;
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
    @NullUnmarked
    public static class State {
        @Param({ "1kb", "32kb", "1mb" })
        String jsonSize;
        @Param({ "unicode" })
        String characters;
        @Param({ "0.1" })
        double maskedKeyProbability;
        @Param({ "false", "true" })
        boolean jsonPath;

        private String jsonString;
        private byte[] jsonBytes;
        private JsonMasker jsonMasker;

        @Setup
        public synchronized void setup() {
            Set<String> targetKeys = BenchmarkUtils.getTargetKeys(20);
            JsonNode randomJson = BenchmarkUtils.randomJson(targetKeys, jsonSize, characters, maskedKeyProbability);
            jsonString = randomJson.toString();
            jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);

            JsonMaskingConfig.Builder builder = JsonMaskingConfig.builder();
            if (jsonPath) {
                builder.maskJsonPaths(BenchmarkUtils.collectJsonPaths(randomJson, targetKeys));
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
