package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;


final class MaskAllKeysTest {
    @ParameterizedTest
    @MethodSource("testMaskAllKeys")
    void maskAllKeysFromFile(JsonMaskerTestInstance testInstance) {
        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(testInstance.jsonMasker(), testInstance.input(),
                testInstance.expectedOutput());
    }

    private static Stream<JsonMaskerTestInstance> testMaskAllKeys() throws IOException {
        return JsonMaskerTestUtil.getJsonMaskerTestInstancesFromFile("test-mask-all-keys.json").stream();
    }

    @Test
    void maskAllKeys() {
        var jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder().allowKeys(Set.of()).build());
        Assertions.assertThat(jsonMasker.mask("{\"someKey\": 0}")).isEqualTo("{\"someKey\": \"###\"}");
    }

    @Test
    void maskAllJsonPaths() {
        var jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder().allowJsonPaths(Set.of()).build());
        Assertions.assertThat(jsonMasker.mask("{\"someKey\": 0}")).isEqualTo("{\"someKey\": \"###\"}");
    }
}

