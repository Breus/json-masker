package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.jspecify.annotations.NullUnmarked;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 1)
@Fork(value = 1)
@Measurement(iterations = 1)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
public class KeyMatcherBenchmark {

    @org.openjdk.jmh.annotations.State(Scope.Thread)
    @NullUnmarked
    public static class State {

        List<String> targetKeys;
        List<byte[]> targetKeysBytes;
        KeyMatcher keyMatcher;
        @Param({ "false", "true" })
        boolean caseSensitive;
        @Param({ "mask", "allow" })
        String mode;

        @Setup
        public synchronized void setup() throws IOException {
            targetKeys = BenchmarkUtils.loadSampleTargetKeys();

            targetKeysBytes = targetKeys.stream().map(key -> key.getBytes(StandardCharsets.UTF_8)).toList();

            var builder = JsonMaskingConfig.builder();
            if (caseSensitive) {
                builder.caseSensitiveTargetKeys();
            }
            if (mode.equals("allow")) {
                builder.allowKeys(new HashSet<>(targetKeys));
            } else {
                builder.maskKeys(new HashSet<>(targetKeys));
            }
            keyMatcher = new KeyMatcher(builder.build());
        }
    }

    @Benchmark
    @OperationsPerInvocation(BenchmarkUtils.TARGET_KEYS_SIZE)
    public void matchAllKeys(Blackhole bh, State state) {
        for (byte[] targetKey : state.targetKeysBytes) {
            var config = state.keyMatcher.getMaskConfigIfMatched(targetKey, 0, targetKey.length, null);
            bh.consume(config);
        }
    }
}
