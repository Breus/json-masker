package dev.blaauwendraad.masker.json.path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

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

    @ParameterizedTest
    @MethodSource("illegalJsonPathLiterals")
    void tryParseIllegalJsonPathLiterals(String literal) {
        JsonPathParser parser = new JsonPathParser();
        Assertions.assertNull(parser.tryParse(literal));
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
    void ambiguousJsonPathKeys(Set<String> jsonPathLiterals, String expectedExceptionMessage) {
        JsonPathParser parser = new JsonPathParser();
        Set<JsonPath> parsedJsonPaths = jsonPathLiterals.stream().map(parser::parse).collect(Collectors.toSet());
        assertThatThrownBy(() -> parser.checkAmbiguity(parsedJsonPaths))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedExceptionMessage);
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
                Arguments.of("$.*.b", new JsonPath(new String[]{"$", "*", "b"})),
                Arguments.of("$", new JsonPath(new String[]{"$"})),
                Arguments.of("$.a.*.*", new JsonPath(new String[]{"$", "a", "*", "*"})),
                Arguments.of("$[*].*.*", new JsonPath(new String[]{"$", "*", "*", "*"}))
        );
    }

    private static Stream<String> illegalJsonPathLiterals() {
        return Stream.of(
                "$..a.b.c",
                "$a.b.c",
                "$a.13.c",
                "$.a.b.*",
                "$.*",
                "$[12].b.c",
                "$.a[?@].b",
                "$.a.'b'.c",
                "$.a.\\..b",
                "$[''][b]",
                "$.a[2:4]",
                "$.a.b[?length(*)<3]",
                "$[a][b][5].c",
                "$.a[0].b[1].c[2]",
                "$.a[0].[b][c]",
                "$[(@.length - 1)].b"
        );
    }

    private static Stream<Arguments> ambiguousJsonPaths() {
        return Stream.of(
                Arguments.of(Set.of("$.a.b.c", "$.a.*.c", "$.i.r.l.v.n.*.t"), "'$.a.*.c' and '$.a.b.c' JSONPath keys combination is not supported: ambiguity at segment 2 with shared path $.a."),
                Arguments.of(Set.of("$.*.b.c", "$.a.b.c"), "'$.*.b.c' and '$.a.b.c' JSONPath keys combination is not supported: ambiguity at segment 1 with shared path $."),
                Arguments.of(Set.of("$.a.b.c", "$.*.*.*"), "'$.*.*.*' and '$.a.b.c' JSONPath keys combination is not supported: ambiguity at segment 1 with shared path $."),
                Arguments.of(Set.of("$.*.b.c", "$.*.b.*.d"), "'$.*.b.*.d' and '$.*.b.c' JSONPath keys combination is not supported: ambiguity at segment 3 with shared path $.*.b."),
                Arguments.of(Set.of("$.a.b.c", "$.a.b.d", "$.a.*.c"), "'$.a.*.c' and '$.a.b.c' JSONPath keys combination is not supported: ambiguity at segment 2 with shared path $.a."),
                Arguments.of(Set.of("$.a.b.c", "$.a.*.c", "$.a.b.s"), "'$.a.*.c' and '$.a.b.c' JSONPath keys combination is not supported: ambiguity at segment 2 with shared path $.a."),
                Arguments.of(Set.of("$.a.*.c", "$.a.b.c", "$.a.b.d"), "'$.a.*.c' and '$.a.b.c' JSONPath keys combination is not supported: ambiguity at segment 2 with shared path $.a."),
                Arguments.of(Set.of("$.a.b.c.f", "$.*.*.*.u"), "'$.*.*.*.u' and '$.a.b.c.f' JSONPath keys combination is not supported: ambiguity at segment 1 with shared path $."),
                Arguments.of(Set.of("$.*.b.c", "$.q.w.e", "$.*.d.f"), "'$.*.d.f' and '$.q.w.e' JSONPath keys combination is not supported: ambiguity at segment 1 with shared path $."),
                Arguments.of(Set.of("$.a.b.c", "$.d.*.*.f", "$.d.*.v.f"), "'$.d.*.*.f' and '$.d.*.v.f' JSONPath keys combination is not supported: ambiguity at segment 3 with shared path $.d.*."),
                Arguments.of(Set.of("$.a.b.*.*.d", "$.a.b.*.c"), "'$.a.b.*.*.d' and '$.a.b.*.c' JSONPath keys combination is not supported: ambiguity at segment 4 with shared path $.a.b.*.")
        );
    }

    private static Stream<Set<String>> notAmbiguousJsonPaths() {
        return Stream.of(
                Set.of("$.a.b.c", "$.d.*.c", "$.f.*.v"),
                Set.of("$.a.b.c", "$.d.*.c", "$.d.*.v"),
                Set.of("$.a.b.c", "$.a.b.d"),
                Set.of("$.a.b.*.d", "$.a.b.*.c"),
                Set.of("$.a.b.*.d", "$.a.b.*.c.f"),
                Set.of("$.ab", "$.a"),
                Set.of("$.a.b", "$.a", "$.a!", "$.a.c", "$.a0.i"),
                Set.of("$.a.b.c", "$.a.b.c.*.f"),
                Set.of("$.key.bbb.c", "$.key.bbb.c.d"),
                Set.of("$.f.e.g", "$.n.*.m", "$", "$.a.b.c.d")
        );
    }

}