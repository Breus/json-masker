package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.util.AssertionsUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

final class EmptyKeyTest {
    @ParameterizedTest
    @MethodSource("emptyKeyTestFile")
    void emptyKey(JsonMaskerTestInstance testInstance) {
        assertThat(testInstance.jsonMasker().mask(testInstance.input())).isEqualTo(testInstance.expectedOutput());
        AssertionsUtil.assertJsonMaskerApiEquivalence(testInstance.jsonMasker(), testInstance.input());
    }

    private static Stream<JsonMaskerTestInstance> emptyKeyTestFile() throws IOException {
        return JsonMaskerTestUtil.getJsonMaskerTestInstancesFromFile("test-empty-key.json").stream();
    }
}
