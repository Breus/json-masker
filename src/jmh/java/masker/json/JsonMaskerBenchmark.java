package masker.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 1, time = 10)
@Fork(value = 1)
@Measurement(iterations = 1, time = 10)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
public class JsonMaskerBenchmark {

    @org.openjdk.jmh.annotations.State(Scope.Thread)
    public static class State {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNode = objectNode();
        String simpleJsonAsString = objectNode.set("someKey", objectNode().set("ab", mapper.convertValue("hello", JsonNode.class))).toString();
        byte[] simpleJsonAsBytes = simpleJsonAsString.getBytes(StandardCharsets.UTF_8);
        String largeJsonAsString = readJsonFromFile();
        byte[] largeJsonAsBytes = largeJsonAsString.getBytes(StandardCharsets.UTF_8);
        JsonMasker defaultMasker = JsonMasker.getMasker("ab");
        JsonMasker twoCharObfuscationLengthMasker = JsonMasker.getMasker("ab", JsonMaskingConfig.custom().obfuscationLength(2).build());
        JsonMasker fiveCharObfuscationLengthMasker = JsonMasker.getMasker("ab", JsonMaskingConfig.custom().obfuscationLength(5).build());
        JsonMasker sixCharObfuscationLengthMasker = JsonMasker.getMasker("ab", JsonMaskingConfig.custom().obfuscationLength(6).build());

        private String readJsonFromFile() {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonObject;
            try {
                jsonObject = mapper.readValue(JsonMaskerBenchmark.class.getClassLoader().getResource("large-input-benchmark.json"), ObjectNode.class);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read benchmark from input file");
            }
            return jsonObject.toString();
        }

        private ObjectNode objectNode() {
            return JsonNodeFactory.instance.objectNode();
        }
    }

    @Benchmark
    public void maskSimpleJsonObjectString(State state, Blackhole blackhole) throws InterruptedException {
        blackhole.consume(state.defaultMasker.mask(state.simpleJsonAsString, "ab"));
    }

    @Benchmark
    public void maskLargeJsonObjectString(State state, Blackhole blackhole) throws InterruptedException {
        blackhole.consume(state.defaultMasker.mask(state.largeJsonAsString, "ab"));
    }

    @Benchmark
    public void maskSimpleJsonObjectBytes(State state, Blackhole blackhole) throws InterruptedException {
        blackhole.consume(state.defaultMasker.mask(state.simpleJsonAsBytes, "ab"));
    }

    @Benchmark
    public void maskLargeJsonObjectBytes(State state, Blackhole blackhole) throws InterruptedException {
        blackhole.consume(state.defaultMasker.mask(state.largeJsonAsBytes, "ab"));
    }

    @Benchmark
    public void maskSimpleJsonObjectObfuscateLengthShorterThanTargetValue(State state, Blackhole blackhole) throws InterruptedException {
        blackhole.consume(state.twoCharObfuscationLengthMasker.mask(state.simpleJsonAsString));
    }

    @Benchmark
    public void maskSimpleJsonObjectObfuscateLengthEqualToTargetValue(State state, Blackhole blackhole) throws InterruptedException {
        blackhole.consume(state.fiveCharObfuscationLengthMasker.mask(state.simpleJsonAsString));
    }

    @Benchmark
    public void maskSimpleJsonObjectObfuscateLengthLongerThanTargetValue(State state, Blackhole blackhole) throws InterruptedException {
        blackhole.consume(state.sixCharObfuscationLengthMasker.mask(state.simpleJsonAsString));
    }

    @Benchmark
    public void parseAndMaskSimpleJsonObjectToString(State state, Blackhole blackhole) throws Exception {
        ObjectNode objectNode = (ObjectNode) state.mapper.readTree(state.simpleJsonAsString);
        objectNode.set("ab", state.mapper.convertValue("*****", JsonNode.class));
        blackhole.consume(objectNode.toString());
    }

    @Benchmark
    public void parseAndMaskLargeJsonObjectToString(State state, Blackhole blackhole) throws Exception {
        ObjectNode objectNode = (ObjectNode) state.mapper.readTree(state.largeJsonAsString);
        objectNode.set("ab", state.mapper.convertValue("*******", JsonNode.class));
        blackhole.consume(objectNode.toString());
    }

    @Benchmark
    public void parseAndMaskSimplesonObjectToBytes(State state, Blackhole blackhole) throws Exception {
        ObjectNode objectNode = (ObjectNode) state.mapper.readTree(state.simpleJsonAsBytes);
        objectNode.set("ab", state.mapper.convertValue("*****", JsonNode.class));
        blackhole.consume(objectNode.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Benchmark
    public void parseAndMaskLargeJsonObjectToBytes(State state, Blackhole blackhole) throws Exception {
        ObjectNode objectNode = (ObjectNode) state.mapper.readTree(state.largeJsonAsBytes);
        objectNode.set("ab", state.mapper.convertValue("*******", JsonNode.class));
        blackhole.consume(objectNode.toString());
    }
}
