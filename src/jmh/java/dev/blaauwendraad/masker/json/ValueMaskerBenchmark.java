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

@Warmup(iterations = 1)
@Fork(value = 1)
@Measurement(iterations = 1)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
public class ValueMaskerBenchmark {


    @org.openjdk.jmh.annotations.State(Scope.Thread)
    @NullUnmarked
    public static class State {
        @Param({ "1kb", "32kb", "1mb" })
        String jsonSize;
        @Param({ "unicode" })
        String characters;
        @Param({ "0.1" })
        double maskedKeyProbability;

        private final JsonMasker nativeMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .maskKeys(Set.of("targetKey"))
                .build()
        );

        private final JsonMasker rawValueMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .maskKeys(Set.of("targetKey"))
                .maskStringsWith(ValueMaskers.withRawValueFunction(value -> "\"***\""))
                .maskNumbersWith(ValueMaskers.withRawValueFunction(value -> "\"###\""))
                .maskBooleansWith(ValueMaskers.withRawValueFunction(value -> "\"&&&\""))
                .build()
        );

        private final JsonMasker textValueMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .maskKeys(Set.of("targetKey"))
                .maskStringsWith(ValueMaskers.withTextFunction(value -> "***"))
                .maskNumbersWith(ValueMaskers.withTextFunction(value -> "###"))
                .maskBooleansWith(ValueMaskers.withTextFunction(value -> "&&&"))
                .build()
        );

        private byte[] jsonBytes;

        @Setup
        public synchronized void setup() {
            JsonNode jsonNode = BenchmarkUtils.randomJson(Set.of("targetKey"), jsonSize, characters, maskedKeyProbability);
            jsonBytes = jsonNode.toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    @Benchmark
    public void maskWithStatic(State state) {
        state.nativeMasker.mask(state.jsonBytes);
    }

    @Benchmark
    public void maskWithRawValueFunction(State state) {
        state.rawValueMasker.mask(state.jsonBytes);
    }

    @Benchmark
    public void maskWithTextValueFunction(State state) {
        state.textValueMasker.mask(state.jsonBytes);
    }
}
