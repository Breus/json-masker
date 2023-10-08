package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.blaauwendraad.masker.json.config.JsonMaskerAlgorithmType;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 1, time = 10)
@Fork(value = 1)
@Measurement(iterations = 1, time = 10)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
public class JsonMaskMultipleTargetKeysBenchmark {
    @org.openjdk.jmh.annotations.State(Scope.Benchmark)
    public static class State {
        Set<String> keysToBeMasked = getTargetKeys(100);
        ObjectMapper mapper = new ObjectMapper();
        String smallJsonAsString = "{\"someSecret\": \"someValue\", \n\"someOtherKey\": {\"someSecret2\": \"value\"}}";
        byte[] simpleJsonAsBytes = smallJsonAsString.getBytes(StandardCharsets.UTF_8);
        String largeJsonAsString =
                ParseAndMaskUtil.readJsonFromFileAsString("large-input-benchmark.json", this.getClass());
        byte[] largeJsonAsBytes = largeJsonAsString.getBytes(StandardCharsets.UTF_8);
        JsonMasker keyContainsJsonMasker = JsonMasker.getMasker(JsonMaskingConfig.custom(keysToBeMasked, JsonMaskingConfig.TargetKeyMode.MASK)
                                                                        .algorithmTypeOverride(JsonMaskerAlgorithmType.KEYS_CONTAIN)
                                                                        .build());
        JsonMasker loopJsonMasker = JsonMasker.getMasker(JsonMaskingConfig.custom(keysToBeMasked,
                                                                                  JsonMaskingConfig.TargetKeyMode.MASK
                )
                                                                 .algorithmTypeOverride(JsonMaskerAlgorithmType.SINGLE_TARGET_LOOP)
                                                                 .build());

        private Set<String> getTargetKeys(int numberOfKeys) {
            Set<String> targetKeys = new HashSet<>();
            for (int i = 0; i < numberOfKeys; i++) {
                targetKeys.add("someSecret" + i);
            }
            return targetKeys;
        }
    }

    @Benchmark
    public void loopMaskMultipleKeysSmallJson(State state, Blackhole blackhole) {
        String maskedJsonOutput = state.loopJsonMasker.mask(state.smallJsonAsString);
        blackhole.consume(maskedJsonOutput);
    }

    @Benchmark
    public void loopMaskMultipleKeysLargeJson(State state, Blackhole blackhole) {
        String maskedJsonOutput = state.loopJsonMasker.mask(state.largeJsonAsString);
        blackhole.consume(maskedJsonOutput);
    }


    @Benchmark
    public void keyContainsMaskMultiKeysSmallJson(State state, Blackhole blackhole) {
        String maskedJsonOutput = state.keyContainsJsonMasker.mask(state.smallJsonAsString);
        blackhole.consume(maskedJsonOutput);
    }

    @Benchmark
    public void keyContainsMaskMultiKeysLargeJson(State state, Blackhole blackhole) {
        String maskedJsonOutput = state.keyContainsJsonMasker.mask(state.largeJsonAsString);
        blackhole.consume(maskedJsonOutput);
    }

    @Benchmark
    public void parseAndMaskMultiKeysSmallJson(State state, Blackhole blackhole) throws IOException {
        String maskedJsonOutput =
                ParseAndMaskUtil.mask(state.simpleJsonAsBytes, state.keysToBeMasked, JsonMaskingConfig.TargetKeyMode.MASK, state.mapper)
                        .toString();
        blackhole.consume(maskedJsonOutput);
    }

    @Benchmark
    public void parseAndMaskMultiKeysLargeJson(State state, Blackhole blackhole) throws IOException {
        String maskedJsonOutput =
                ParseAndMaskUtil.mask(state.largeJsonAsBytes, state.keysToBeMasked, JsonMaskingConfig.TargetKeyMode.MASK, state.mapper)
                        .toString();
        blackhole.consume(maskedJsonOutput);
    }
}