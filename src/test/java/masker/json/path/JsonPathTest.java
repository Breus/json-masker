package masker.json.path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class JsonPathTest {

    @ParameterizedTest
    @MethodSource("validJsonPaths")
    void jsonPathParsing(String jsonPathLiteral) {
        Assertions.assertEquals(jsonPathLiteral, JsonPath.from(jsonPathLiteral).toString());
    }

    private static Stream<String> validJsonPaths() {
        return Stream.of(
                "$.a.b",
                "$.a",
                "$.a[0].b",
                "$.a.b.c[1].d"
        );
    }
}
