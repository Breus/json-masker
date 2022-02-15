package masker.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 1)
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
    public String maskSimpleJsonObjectString(State state) throws InterruptedException {
        return state.defaultMasker.mask(state.simpleJsonAsString, "ab");
    }

    @Benchmark
    public String maskLargeJsonObjectString(State state) throws InterruptedException {
        return state.defaultMasker.mask(state.largeJsonAsString, "ab");
    }

    @Benchmark
    public byte[] maskSimpleJsonObjectBytes(State state) throws InterruptedException {
        return state.defaultMasker.mask(state.simpleJsonAsBytes, "ab");
    }

    @Benchmark
    public byte[] maskLargeJsonObjectBytes(State state) throws InterruptedException {
        return state.defaultMasker.mask(state.largeJsonAsBytes, "ab");
    }

    @Benchmark
    public String maskSimpleJsonObjectObfuscateLengthShorterThanTargetValue(State state) throws InterruptedException {
        return state.twoCharObfuscationLengthMasker.mask(state.simpleJsonAsString);
    }

    @Benchmark
    public String maskSimpleJsonObjectObfuscateLengthEqualToTargetValue(State state) throws InterruptedException {
        return state.fiveCharObfuscationLengthMasker.mask(state.simpleJsonAsString);
    }

    @Benchmark
    public String maskSimpleJsonObjectObfuscateLengthLongerThanTargetValue(State state) throws InterruptedException {
        return state.sixCharObfuscationLengthMasker.mask(state.simpleJsonAsString);
    }

    @Benchmark
    public String parseAndMaskSmallJsonObjectToString(State state) throws Exception {
        ObjectNode objectNode = (ObjectNode) state.mapper.readTree(state.simpleJsonAsString);
        objectNode.set("ab", state.mapper.convertValue("*****", JsonNode.class));
        return objectNode.toString();
    }

    @Benchmark
    public String parseAndMaskLargeJsonObjectToString(State state) throws Exception {
        ObjectNode objectNode = (ObjectNode) state.mapper.readTree(state.largeJsonAsString);
        objectNode.set("ab", state.mapper.convertValue("*******", JsonNode.class));
        return objectNode.toString();
    }

    @Benchmark
    public byte[] parseAndMaskSmallJsonObjectToBytes(State state) throws Exception {
        ObjectNode objectNode = (ObjectNode) state.mapper.readTree(state.simpleJsonAsBytes);
        objectNode.set("ab", state.mapper.convertValue("*****", JsonNode.class));
        return objectNode.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Benchmark
    public String parseAndMaskLargeJsonObjectToBytes(State state) throws Exception {
        ObjectNode objectNode = (ObjectNode) state.mapper.readTree(state.largeJsonAsBytes);
        objectNode.set("ab", state.mapper.convertValue("*******", JsonNode.class));
        return objectNode.toString();
    }
}
