package dev.blaauwendraad.masker.json.path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Collectors;
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

    @Test
    void twoJsonPathAreEqual() {
        JsonPathParser parser = new JsonPathParser();
        JsonPath bracketNotationJsonPath = parser.parse("$[a][b]");
        JsonPath dotNotationJsonPath = parser.parse("$.a.b");
        Assertions.assertEquals(bracketNotationJsonPath, dotNotationJsonPath);
    }

    @ParameterizedTest
    @MethodSource("ambiguousJsonPaths")
    void ambiguousJsonPathKeys(Set<String> jsonPathLiterals) {
        JsonPathParser parser = new JsonPathParser();
        Set<JsonPath> parsedJsonPaths = jsonPathLiterals.stream().map(parser::parse).collect(Collectors.toSet());
        Assertions.assertThrows(IllegalArgumentException.class, () -> parser.checkAmbiguity(parsedJsonPaths));
    }

    @ParameterizedTest
    @MethodSource("notAmbiguousJsonPaths")
    void notAmbiguousJsonPathKeys(Set<String> jsonPathLiterals) {
        JsonPathParser parser = new JsonPathParser();
        Set<JsonPath> parsedJsonPaths = jsonPathLiterals.stream().map(parser::parse).collect(Collectors.toSet());
        parser.checkAmbiguity(parsedJsonPaths);
        Assertions.assertDoesNotThrow(() -> parser.checkAmbiguity(parsedJsonPaths));
    }

    private static Stream<Arguments> legalJsonPathLiterals() {
        return Stream.of(
                Arguments.of("$.a", new JsonPath(new String[]{"$", "a"})),
                Arguments.of("$[a][b][c]", new JsonPath(new String[]{"$", "a", "b", "c"})),
                Arguments.of("$.a.b.c", new JsonPath(new String[]{"$", "a", "b", "c"})),
                Arguments.of("$.a.b.c.", new JsonPath(new String[]{"$", "a", "b", "c"})),
                Arguments.of("$[a].b.[c]", new JsonPath(new String[]{"$", "a", "b", "c"})),
                Arguments.of("$.a.[b][c]", new JsonPath(new String[]{"$", "a", "b", "c"})),
                Arguments.of("$[][][]", new JsonPath(new String[]{"$", "", "", ""})),
                Arguments.of("$.a.*.c", new JsonPath(new String[]{"$", "a", "*", "c"})),
                Arguments.of("$.*.b.*", new JsonPath(new String[]{"$", "*", "b", "*"})),
                Arguments.of("$[*].*.*", new JsonPath(new String[]{"$", "*", "*", "*"}))
        );
    }

    private static Stream<String> illegalJsonPathLiterals() {
        return Stream.of(
                "$..a.b.c",
                "$a.b.c",
                "$a.13.c",
                "$[12].b.c",
                "$.a[?@].b",
                "$.a.'b'.c",
                "$.a.\\..b",
                "$[''][b]",
                "$.a[2:4]",
                "$.a.b[?length(*)<3]",
                "$[a][b][5].c",
                "$.a[0].b[1].c[2]",
                "$.a[0].[b][c]"
        );
    }

    private static Stream<Set<String>> ambiguousJsonPaths() {
        return Stream.of(
                Set.of("$.a.b.c", "$.a.*.c"),
                Set.of("$.*.b.c", "$.a.b.c"),
                Set.of("$.a.b.c", "$.*.*.*"),
                Set.of("$.*.b.c", "$.*.b.*"),
                Set.of("$.a.b.c", "$.a.b.d", "$.a.*.c"),
                Set.of("$.a.b.c", "$.a.*.c", "$.a.b.s"),
                Set.of("$.a.*.c", "$.a.b.c", "$.a.b.d"),
                Set.of("$.a.b.c.f", "$.*.*.*.u"),
                Set.of("$.*.b.c", "$.q.w.e", "$.*.d.f"),
                Set.of("$.a.b.c", "$.d.*.*.f", "$.d.*.v.f")
        );
    }

    private static Stream<Set<String>> notAmbiguousJsonPaths() {
        return Stream.of(
                Set.of("$.a.b.c", "$.d.*.c", "$.f.*.v"),
                Set.of("$.a.b.c", "$.a.d.*", "$.a[b]d"),
                Set.of("$.a.b.*", "$.a.b.*.c"),
                Set.of("$.a.b.c", "$.a.b.c.*"),
                Set.of("$.a.b.c", "$.d.*.c", "$.d.*.v")
        );
    }

}