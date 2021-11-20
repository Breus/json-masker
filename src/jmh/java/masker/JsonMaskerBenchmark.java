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
        String inputString = objectNode().set("cab", objectNode().set("ab", mapper.convertValue("hello", JsonNode.class))).toString();
    }

    @Benchmark
    public String maskSimpleJsonObject(State state) throws Exception {
        return JsonMasker.getMaskerWithTargetKey("ab").mask(state.inputString);
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
