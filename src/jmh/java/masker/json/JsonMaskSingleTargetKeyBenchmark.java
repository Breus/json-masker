package masker.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 1, time = 10)
@Fork(value = 1)
@Measurement(iterations = 1, time = 10)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
public class JsonMaskSingleTargetKeyBenchmark {

    @org.openjdk.jmh.annotations.State(Scope.Thread)
    public static class State {
        String keyToBeMasked = "someSecret";

        ObjectMapper mapper = new ObjectMapper();

        String simpleJsonAsString = objectNode().set("someKey", objectNode().put("someSecret", "hello")).toString();
        byte[] simpleJsonAsBytes = simpleJsonAsString.getBytes(StandardCharsets.UTF_8);

        String largeJsonAsString = ParseAndMaskUtil.readJsonFromFileAsString("large-input-benchmark.json", this.getClass());
        byte[] largeJsonAsBytes = largeJsonAsString.getBytes(StandardCharsets.UTF_8);

        JsonMasker defaultMasker = JsonMasker.getMasker(keyToBeMasked);
        JsonMasker twoCharObfuscationLengthMasker = JsonMasker.getMasker(keyToBeMasked, JsonMaskingConfig.custom().obfuscationLength(2).build());
        JsonMasker fiveCharObfuscationLengthMasker = JsonMasker.getMasker(keyToBeMasked, JsonMaskingConfig.custom().obfuscationLength(5).build());
        JsonMasker sixCharObfuscationLengthMasker = JsonMasker.getMasker(keyToBeMasked, JsonMaskingConfig.custom().obfuscationLength(6).build());

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
        blackhole.consume(state.defaultMasker.mask(state.simpleJsonAsBytes, StandardCharsets.UTF_8));
    }

    @Benchmark
    public void maskLargeJsonObjectBytes(State state, Blackhole blackhole) {
        blackhole.consume(state.defaultMasker.mask(state.largeJsonAsBytes, StandardCharsets.UTF_8));
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
        JsonNode jsonNode = ParseAndMaskUtil.parseBytesAndMask(state.simpleJsonAsBytes, state.keyToBeMasked, state.mapper);
        blackhole.consume(jsonNode);
    }

    @Benchmark
    public void parseAndMaskSmallJsonObjectAsString(State state, Blackhole blackhole) throws Exception {
        JsonNode jsonNode = ParseAndMaskUtil.parseStringAndMask(state.simpleJsonAsString, state.keyToBeMasked, state.mapper);
        blackhole.consume(jsonNode);
    }

    @Benchmark
    public void parseAndMaskLargeJsonObjectAsString(State state, Blackhole blackhole) throws Exception {
        JsonNode jsonNode = ParseAndMaskUtil.parseStringAndMask(state.largeJsonAsString, state.keyToBeMasked, state.mapper);
        blackhole.consume(jsonNode);
    }

    @Benchmark
    public void parseAndMaskLargeJsonObjectAsBytes(State state, Blackhole blackhole) throws Exception {
        JsonNode jsonNode = ParseAndMaskUtil.parseBytesAndMask(state.largeJsonAsBytes, state.keyToBeMasked, state.mapper);
        blackhole.consume(jsonNode);
    }
}
