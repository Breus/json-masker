package dev.blaauwendraad.masker.json;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

final class MultiTargetKeyTest {
    @ParameterizedTest
    @MethodSource("multiTargetKeyFile")
    void multiTargetKey(JsonMaskerTestInstance testInstance) {
        assertThat(testInstance.jsonMasker().mask(testInstance.input())).isEqualTo(testInstance.expectedOutput());
    }

    private static Stream<JsonMaskerTestInstance> multiTargetKeyFile() throws IOException {
        return JsonMaskerTestUtil.getJsonMaskerTestInstancesFromFile("test-multiple-target-keys.json").stream();
    }
}
