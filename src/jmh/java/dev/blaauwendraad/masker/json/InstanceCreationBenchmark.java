package dev.blaauwendraad.masker.json;

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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Warmup(iterations = 1)
@Fork(value = 1)
@Measurement(iterations = 1)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
public class InstanceCreationBenchmark {

    @org.openjdk.jmh.annotations.State(Scope.Thread)
    @NullUnmarked
    public static class State {
        @Param({"10", "500", "" + BenchmarkUtils.TARGET_KEYS_SIZE})
        int numberOfTargetKeys;

        Set<String> targetKeys;

        @Setup
        public synchronized void setup() {
            List<String> targetKeyList = BenchmarkUtils.loadSampleTargetKeys();
            if (numberOfTargetKeys <= targetKeyList.size()) {
                Collections.shuffle(targetKeyList, new Random(BenchmarkUtils.STATIC_RANDOM_SEED));
                targetKeys = targetKeyList.stream().limit(numberOfTargetKeys).collect(Collectors.toSet());
            } else {
                targetKeys = new HashSet<>(targetKeyList);
            }
        }
    }

    @Benchmark
    public JsonMasker jsonMasker(State state) {
        return JsonMasker.getMasker(state.targetKeys);
    }
}
