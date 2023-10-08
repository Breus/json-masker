package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 1, time = 3)
@Fork(value = 1)
@Measurement(iterations = 1, time = 3)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
public class JsonMaskSingleTargetKeyBenchmark {

    @org.openjdk.jmh.annotations.State(Scope.Thread)
    public static class State {
        String keyToBeMasked = "someSecret";

        ObjectMapper mapper = new ObjectMapper();

        String simpleJsonAsString = objectNode().set("someKey", objectNode().put("someSecret", "hello")).toString();
        byte[] simpleJsonAsBytes = simpleJsonAsString.getBytes(StandardCharsets.UTF_8);

        String largeJsonAsString =
                ParseAndMaskUtil.readJsonFromFileAsString("large-input-benchmark.json", this.getClass());
        byte[] largeJsonAsBytes = largeJsonAsString.getBytes(StandardCharsets.UTF_8);

        JsonMasker defaultMasker = JsonMasker.getMasker(keyToBeMasked);
        JsonMasker twoCharObfuscationLengthMasker =
                JsonMasker.getMasker(JsonMaskingConfig.custom(Set.of(keyToBeMasked),
                                                              JsonMaskingConfig.TargetKeyMode.MASK).obfuscationLength(2).build());
        JsonMasker fiveCharObfuscationLengthMasker =
                JsonMasker.getMasker(JsonMaskingConfig.custom(Set.of(keyToBeMasked),
                                                              JsonMaskingConfig.TargetKeyMode.MASK).obfuscationLength(5).build());
        JsonMasker sixCharObfuscationLengthMasker =
                JsonMasker.getMasker(JsonMaskingConfig.custom(Set.of(keyToBeMasked),
                                                              JsonMaskingConfig.TargetKeyMode.MASK).obfuscationLength(6).build());

        private ObjectNode objectNode() {
            return JsonNodeFactory.instance.objectNode();
        }
    }

    @Benchmark
    public void maskSimpleJsonObjectString(State state, Blackhole blackhole) {
        blackhole.consume(state.defaultMasker.mask(state.simpleJsonAsString));
    }

    @Benchmark
    public void maskLargeJsonObjectString(State state, Blackhole blackhole) {
        blackhole.consume(state.defaultMasker.mask(state.largeJsonAsString));
    }

    @Benchmark
    public void maskSimpleJsonObjectBytes(State state, Blackhole blackhole) {
        blackhole.consume(state.defaultMasker.mask(state.simpleJsonAsBytes));
    }

    @Benchmark
    public void maskLargeJsonObjectBytes(State state, Blackhole blackhole) {
        blackhole.consume(state.defaultMasker.mask(state.largeJsonAsBytes));
    }

    @Benchmark
    public void maskSimpleJsonObjectObfuscateLengthShorterThanTargetValue(State state, Blackhole blackhole) {
        blackhole.consume(state.twoCharObfuscationLengthMasker.mask(state.simpleJsonAsString));
    }

    @Benchmark
    public void maskSimpleJsonObjectObfuscateLengthEqualToTargetValue(State state, Blackhole blackhole) {
        blackhole.consume(state.fiveCharObfuscationLengthMasker.mask(state.simpleJsonAsString));
    }

    @Benchmark
    public void maskSimpleJsonObjectObfuscateLengthLongerThanTargetValue(State state, Blackhole blackhole) {
        blackhole.consume(state.sixCharObfuscationLengthMasker.mask(state.simpleJsonAsString));
    }

    @Benchmark
    public void parseAndMaskSmallJsonObjectAsByte(State state, Blackhole blackhole) throws Exception {
        JsonNode jsonNode =
                ParseAndMaskUtil.mask(state.simpleJsonAsBytes, state.keyToBeMasked, JsonMaskingConfig.TargetKeyMode.MASK, state.mapper);
        blackhole.consume(jsonNode);
    }

    @Benchmark
    public void parseAndMaskSmallJsonObjectAsString(State state, Blackhole blackhole) throws Exception {
        JsonNode jsonNode =
                ParseAndMaskUtil.mask(state.simpleJsonAsString, state.keyToBeMasked, JsonMaskingConfig.TargetKeyMode.MASK, state.mapper);
        blackhole.consume(jsonNode);
    }

    @Benchmark
    public void parseAndMaskLargeJsonObjectAsString(State state, Blackhole blackhole) throws Exception {
        JsonNode jsonNode =
                ParseAndMaskUtil.mask(state.largeJsonAsString, state.keyToBeMasked, JsonMaskingConfig.TargetKeyMode.MASK, state.mapper);
        blackhole.consume(jsonNode);
    }

    @Benchmark
    public void parseAndMaskLargeJsonObjectAsBytes(State state, Blackhole blackhole) throws Exception {
        JsonNode jsonNode =
                ParseAndMaskUtil.mask(state.largeJsonAsBytes, state.keyToBeMasked, JsonMaskingConfig.TargetKeyMode.MASK, state.mapper);
        blackhole.consume(jsonNode);
    }
}
