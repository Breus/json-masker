package dev.blaauwendraad.masker.json;

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
import randomgen.json.JsonPathTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
        @Param({ "none", "8" })
        String obfuscationLength;
        @Param({"false", "true"})
        String disableJsonPathResolving;

        private String jsonString;
        private byte[] jsonBytes;
        private JsonMasker jsonMasker;

        @Setup
        public synchronized void setup() {
            Set<String> targetKeys = BenchmarkUtils.getTargetKeys(20);
            jsonString = BenchmarkUtils.randomJson(targetKeys, jsonSize, characters, maskedKeyProbability);
            jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);

            if (!Boolean.parseBoolean(disableJsonPathResolving)) {
                // transform random half of keys into json path keys
                Set<String> halfTargetKeys = selectRandomHalf(targetKeys);
                targetKeys.removeAll(halfTargetKeys);
                targetKeys.addAll(JsonPathTestUtils.transformToJsonPathKeys(halfTargetKeys, jsonString));
            }

            JsonMaskingConfig.Builder jsonMaskedBuilder = JsonMaskingConfig.custom(targetKeys, JsonMaskingConfig.TargetKeyMode.MASK)
                    .obfuscationLength(Objects.equals(obfuscationLength, "none")
                            ? -1
                            : Integer.parseInt(obfuscationLength));
            if (Boolean.parseBoolean(disableJsonPathResolving)) {
                jsonMaskedBuilder = jsonMaskedBuilder.disableJsonPathResolving();
            }
            jsonMasker = JsonMasker.getMasker(jsonMaskedBuilder.build());
        }

        private <T> Set<T> selectRandomHalf(Set<T> set) {
            List<T> list = new ArrayList<>(new HashSet<>(set).stream().toList());
            Collections.shuffle(list);
            return list.stream().limit(set.size()/2).collect(Collectors.toSet());
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
