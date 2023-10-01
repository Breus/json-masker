package masker.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

final class IndexOfMethodTest {

    @ParameterizedTest
    @MethodSource("indexOfArguments")
    void indexOf(String src, String target, int expectedIndexOf) {
        Assertions.assertEquals(expectedIndexOf,
                                SingleTargetMasker.indexOf(src.getBytes(StandardCharsets.UTF_8),
                                                           target.getBytes(StandardCharsets.UTF_8)));
    }

    static Stream<Arguments> indexOfArguments() {
        return Stream.of(
                Arguments.of("hello", "e", 1),
                Arguments.of("hello", "a", -1),
                Arguments.of("hello", "", 0),
                Arguments.of("hello", "hello", 0),
                Arguments.of("hello", "he", 0),
                Arguments.of("hello", "o", 4),
                Arguments.of("hello", "oo", -1)
        );
    }
}
