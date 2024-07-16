package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.blaauwendraad.masker.randomgen.RandomJsonGenerator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class InstanceCreationMemoryUsageTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void defaultInstanceCreation() throws IOException {
        URL targetKeyFileUrl = RandomJsonGenerator.class.getResource("/target_keys.json");
        Set<String> targetKeys = new HashSet<>();
        objectMapper.readValue(targetKeyFileUrl, ArrayNode.class).forEach(t -> targetKeys.add(t.textValue()));

        long memoryBeforeInstanceCreation = getCurrentRetainedMemory();

        JsonMasker masker = JsonMasker.getMasker(targetKeys);
        masker.mask("{\"maskMe\": \"secret\"}"); // just run once to make sure there's no side effects

        long memoryAfterInstanceCreation = getCurrentRetainedMemory();

        long memoryLimit = 10_000;
        long memoryConsumed = bytesToKb(memoryAfterInstanceCreation - memoryBeforeInstanceCreation);

        Assertions.assertThat(memoryConsumed)
                .withFailMessage("Expected to use less than %sKB of memory, got %sKB", memoryLimit, memoryConsumed)
                .isLessThan(memoryLimit);
    }

    private long getCurrentRetainedMemory() {
        Runtime.getRuntime().gc(); // run garbage collector
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    private long bytesToKb(long bytes) {
        return bytes / 1024;
    }
}
