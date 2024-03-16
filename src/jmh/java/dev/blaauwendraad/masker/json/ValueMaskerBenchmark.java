package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;
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
public class ValueMaskerBenchmark {


    @org.openjdk.jmh.annotations.State(Scope.Thread)
    public static class State {

        @Param({ "1kb", "128kb" })
        String jsonSize;
        @Param({ "unicode" })
        String characters;
        @Param({ "0.1" })
        double maskedKeyProbability;

        private final JsonMasker nativeMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .maskKeys(Set.of("targetKey"), KeyMaskingConfig.builder()
                        .maskStringsWith(ValueMasker.maskWith("***"))
                        .maskNumbersWith(ValueMasker.maskWith("###"))
                        .maskBooleansWith(ValueMasker.maskWith("&&&"))
                        .build())
                .build()
        );
        private final JsonMasker functionalMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .maskKeys(Set.of("targetKey"), KeyMaskingConfig.builder()
                        .maskStringsWith(ValueMasker.maskWithStringFunction(context -> "***"))
                        .maskNumbersWith(ValueMasker.maskWithStringFunction(context -> "###"))
                        .maskBooleansWith(ValueMasker.maskWithStringFunction(context -> "&&&"))
                        .build())
                .build()
        );

        private byte[] jsonBytes;

        @Setup
        public synchronized void setup() {
            String jsonString = BenchmarkUtils.randomJson(Set.of("targetKey"), jsonSize, characters, maskedKeyProbability);
            jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);
        }
    }

    @Benchmark
    public void maskWithStatic(State state) {
        state.nativeMasker.mask(state.jsonBytes);
    }

    @Benchmark
    public void maskWithFunctional(State state) {
        state.functionalMasker.mask(state.jsonBytes);
    }
}
