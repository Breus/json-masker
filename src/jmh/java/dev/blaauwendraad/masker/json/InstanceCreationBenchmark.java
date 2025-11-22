package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.randomgen.RandomJsonGenerator;
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
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Warmup(iterations = 1, time = 3)
@Fork(value = 1)
@Measurement(iterations = 1, time = 3)
 @OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
public class InstanceCreationBenchmark {
    private static final JsonMapper jsonMapper = new JsonMapper();

    @org.openjdk.jmh.annotations.State(Scope.Thread)
    @NullUnmarked
    public static class State {
        @Param({"1", "10", "100", "ALL"})
        String numberOfTargetKeys;

        Set<String> targetKeys;

        @Setup
        public synchronized void setup() throws IOException {
            try (var stream = RandomJsonGenerator.class.getResourceAsStream("/target_keys.json")) {
                if (stream == null) {
                    throw new IllegalArgumentException("File not found: target_keys.json");
                }
                List<String> targetKeyList = new ArrayList<>();
                jsonMapper.readValue(stream, ArrayNode.class).forEach(t -> targetKeyList.add(t.asString()));
                if (!"ALL".equals(numberOfTargetKeys) && Integer.parseInt(numberOfTargetKeys) <= targetKeyList.size()) {
                    Collections.shuffle(targetKeyList, new Random(RandomJsonGenerator.STATIC_RANDOM_SEED));
                    targetKeys =
                            targetKeyList.stream().limit(Integer.parseInt(numberOfTargetKeys)).collect(Collectors.toSet());
                } else {
                    targetKeys = targetKeyList.stream().collect(Collectors.toSet());
                }
            }
        }
    }

    @Benchmark
    public JsonMasker jsonMasker(State state) {
        return JsonMasker.getMasker(state.targetKeys);
    }
}
