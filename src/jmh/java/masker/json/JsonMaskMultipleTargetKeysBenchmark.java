package masker.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 1, time = 1)
@Fork(value = 1)
@Measurement(iterations = 1, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
public class JsonMaskMultipleTargetKeysBenchmark {
    @org.openjdk.jmh.annotations.State(Scope.Benchmark)
    public static class State {
        Set<String> keysToBeMasked = getKeysToBeMasked(100);
        ObjectMapper mapper = new ObjectMapper();
        String simpleJsonObjectAsString = "{\"someSecret\": \"someValue\", \n\"someOtherKey\": {\"someSecret2\": \"value\"}}";
        byte[] simpleJsonAsBytes = simpleJsonObjectAsString.getBytes(StandardCharsets.UTF_8);
        String largeJsonAsString = ParseAndMaskUtil.readJsonFromFileAsString("large-input-benchmark.json", this.getClass());
        byte[] largeJsonAsBytes = largeJsonAsString.getBytes(StandardCharsets.UTF_8);
        JsonMasker jsonMasker = JsonMasker.getMasker(keysToBeMasked, JsonMaskingConfig.custom().multiTargetAlgorithm(JsonMultiTargetAlgorithm.KEYS_CONTAIN).build());

        private Set<String> getKeysToBeMasked(int numberOfKeys) {
            Set<String> keysToBeMasked = new HashSet<>();
            for (int i = 0; i < numberOfKeys; i++) {
                keysToBeMasked.add("someSecret" + i);
            }
            return keysToBeMasked;
        }
    }

    @Benchmark
    public void maskMultipleKeysInSimpleJsonObject(State state, Blackhole blackhole) {
        String maskedJsonOutput = state.jsonMasker.mask(state.simpleJsonObjectAsString);
        blackhole.consume(maskedJsonOutput);
    }

    @Benchmark
    public void maskMultipleKeysInLargeJsonObject(State state, Blackhole blackhole) {
        String maskedJsonOutput = state.jsonMasker.mask(state.largeJsonAsString);
        blackhole.consume(maskedJsonOutput);
    }

    @Benchmark
    public void parseAndMaskMultipleKeysInSimpleJsonObject(State state, Blackhole blackhole) throws IOException {
        String maskedJsonOutput = ParseAndMaskUtil.parseBytesAndMask(state.simpleJsonAsBytes, state.keysToBeMasked, state.mapper).toString();
        blackhole.consume(maskedJsonOutput);
    }

    @Benchmark
    public void parseAndMaskMultipleKeysInLargeJsonObject(State state, Blackhole blackhole) throws IOException {
        String maskedJsonOutput = ParseAndMaskUtil.parseBytesAndMask(state.largeJsonAsBytes, state.keysToBeMasked, state.mapper).toString();
        blackhole.consume(maskedJsonOutput);
    }
}
