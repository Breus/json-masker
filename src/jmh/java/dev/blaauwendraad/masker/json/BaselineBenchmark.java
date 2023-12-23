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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Warmup(iterations = 1, time = 3)
@Fork(value = 1)
@Measurement(iterations = 1, time = 3)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
public class BaselineBenchmark {

    @org.openjdk.jmh.annotations.State(Scope.Thread)
    public static class State {
        @Param({ "1kb", "128kb", "2mb" })
        String jsonSize;
        @Param({ "unicode" })
        String characters;
        @Param({ "0.01" })
        double maskedKeyProbability;

        private Set<String> targetKeys;
        private String jsonString;
        private byte[] jsonBytes;
        private List<Pattern> regexList;

        @Setup
        public synchronized void setup() {
            targetKeys = BenchmarkUtils.getTargetKeys(20);
            jsonString = BenchmarkUtils.randomJson(targetKeys, jsonSize, characters, maskedKeyProbability);
            jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);

            regexList = targetKeys.stream()
                    // will only match primitive values, not objects or arrays, but it's good to show the difference
                    .map(key -> Pattern.compile("(\"" + key + "\"\\s*:\\s*)(\"?[^\"]*\"?)", Pattern.CASE_INSENSITIVE))
                    .collect(Collectors.toList());
        }
    }

    @Benchmark
    public int countBytes(State state) {
        int sum = 0;
        for (int i = 0; i < state.jsonBytes.length; i++) {
            sum += state.jsonBytes[i];
        }
        return sum;
    }

    @Benchmark
    public String regexReplace(State state) {
        String masked = state.jsonString;
        for (Pattern pattern : state.regexList) {
            Matcher matcher = pattern.matcher(masked);
            if (matcher.find()) {
                masked = matcher.replaceAll(matchResult -> {
                    String beforeValuePart = matchResult.group(1);
                    String value = matchResult.group(2);
                    int maskCount = value.startsWith("\"") ? value.length() - 2 : value.length();
                    return beforeValuePart + "*".repeat(maskCount);
                });
            }
        }
        return masked;
    }

    @Benchmark
    public String jacksonParseAndMask(State state) throws IOException {
        return ParseAndMaskUtil.mask(
                state.jsonString,
                JsonMaskingConfig.getDefault(state.targetKeys)
        ).toString();
    }
}
