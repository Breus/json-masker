package dev.blaauwendraad.masker.json;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class ArrayInputTest {
	@ParameterizedTest
	@MethodSource("testArrayInputFile")
	void arrayInput(JsonMaskerTestInstance testInstance) {
		assertThat(testInstance.jsonMasker().mask(testInstance.input())).isEqualTo(testInstance.expectedOutput());
	}

	private static Stream<JsonMaskerTestInstance> testArrayInputFile() throws IOException {
		return JsonMaskerTestUtil.getJsonMaskerTestInstancesFromFile("test-array-input.json").stream();
	}
}
