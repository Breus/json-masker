package masker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openjdk.jmh.annotations.*;

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
        String inputString = objectNode.set("someKey", objectNode().set("ab", mapper.convertValue("hello", JsonNode.class))).toString();
        JsonMasker defaultMasker = JsonMasker.getDefaultMasker("ab");
        JsonMasker twoCharObfuscationLengthMasker = JsonMasker.getMasker("ab", MaskingConfig.custom().obfuscationLength(2).build());
        JsonMasker fiveCharObfuscationLengthMasker = JsonMasker.getMasker("ab", MaskingConfig.custom().obfuscationLength(5).build());
        JsonMasker sixCharObfuscationLengthMasker = JsonMasker.getMasker("ab", MaskingConfig.custom().obfuscationLength(6).build());
    }

    @Benchmark
    public String maskSimpleJsonObject(State state) throws InterruptedException {
        return state.defaultMasker.mask(state.inputString);
    }

    @Benchmark
    public String maskSimpleJsonObjectObfuscateLengthShorterThanTargetValue(State state) throws InterruptedException {
        return state.twoCharObfuscationLengthMasker.mask(state.inputString);
    }

    @Benchmark
    public String maskSimpleJsonObjectObfuscateLengthEqualToTargetValue(State state) throws InterruptedException {
        return state.fiveCharObfuscationLengthMasker.mask(state.inputString);
    }

    @Benchmark
    public String maskSimpleJsonObjectObfuscateLengthLongerThanTargetValue(State state) throws InterruptedException {
        return state.sixCharObfuscationLengthMasker.mask(state.inputString);
    }

    @Benchmark
    public String parseAndMaskJsonObject(State state) throws Exception {
        ObjectNode objectNode = (ObjectNode) state.mapper.readTree(state.inputString);
        objectNode.set("ab", state.mapper.convertValue("*****", JsonNode.class));
        return objectNode.toString();
    }

    private static ObjectNode objectNode() {
        return JsonNodeFactory.instance.objectNode();
    }
}
