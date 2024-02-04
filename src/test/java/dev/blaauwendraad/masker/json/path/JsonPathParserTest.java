package dev.blaauwendraad.masker.json.path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class JsonPathParserTest {

    @ParameterizedTest
    @MethodSource("legalJsonPathLiterals")
    void parseLegalJsonPathLiterals(String literal, JsonPath expected) {
        JsonPathParser parser = new JsonPathParser();
        Assertions.assertEquals(expected, parser.parse(literal));
    }

    @ParameterizedTest
    @MethodSource("illegalJsonPathLiterals")
    void parseIllegalJsonPathLiterals(String literal) {
        JsonPathParser parser = new JsonPathParser();
        Assertions.assertThrows(IllegalArgumentException.class, () -> parser.parse(literal));
    }

    private static Stream<Arguments> legalJsonPathLiterals() {
        return Stream.of(
                Arguments.of("$.a", new JsonPath(new String[]{"$", "a"})),
                Arguments.of("$[a][b][c]", new JsonPath(new String[]{"$", "a", "b", "c"})),
                Arguments.of("$.a.b.c", new JsonPath(new String[]{"$", "a", "b", "c"})),
                Arguments.of("$.a.b.c.", new JsonPath(new String[]{"$", "a", "b", "c"})),
                Arguments.of("$[a].b.[c]", new JsonPath(new String[]{"$", "a", "b", "c"})),
                Arguments.of("$.a.[b][c]", new JsonPath(new String[]{"$", "a", "b", "c"})),
                Arguments.of("$.a[0].[b][c]", new JsonPath(new String[]{"$", "a", "0", "b", "c"})),
                Arguments.of("$[][][]", new JsonPath(new String[]{"$", "", "", ""})),
                Arguments.of("$[a][b][5].c", new JsonPath(new String[]{"$", "a", "b", "5", "c"})),
                Arguments.of("$.a[0].b[1].c[2]", new JsonPath(new String[]{"$", "a", "0", "b", "1", "c", "2"}))
        );
    }

    private static Stream<String> illegalJsonPathLiterals() {
        return Stream.of(
                "$..a.b.c",
                "$.a[?@].b",
                "$.a.'b'.c",
                "$.a.\\..b",
                "$[''][b]",
                "$.a.*.b",
                "$.a[2:4]",
                "$.a.b[?length(*)<3]"
        );
    }

}