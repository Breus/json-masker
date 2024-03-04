package dev.blaauwendraad.masker.json.path;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Parses a jsonpath literal into a {@link dev.blaauwendraad.masker.json.path.JsonPath} object.
 * <p>
 * The following features from jsonpath specification are not supported:
 * <ul>
 *  <li>Descendant segments</li>
 *  <li>Wildcard selectors</li>
 *  <li>Array slice selectors</li>
 *  <li>Filter selectors</li>
 *  <li>Function extensions</li>
 *  <li>Escape characters</li>
 * </ul>
 * <p>
 * The parser makes a couple of additional restrictions:
 * <ul>
 *  <li>Numbers as key names are disallowed</li>
 *  <li>A set of input jsonpath literals must not be ambiguous</li>
 * </ul>
 * An example of ambiguous set of queries is {@code $.*.b} and {@code $.a.b}. In this case, we cannot match forward the segments.
 */
public class JsonPathParser {

    /**
     * Parses an input literal into a {@link dev.blaauwendraad.masker.json.path.JsonPath} object.
     * Throws {@link java.lang.IllegalArgumentException} when the input literal does not follow the jsonpath specification.
     *
     * @param literal a jsonpath literal to be parsed.
     * @return {@link dev.blaauwendraad.masker.json.path.JsonPath} object parsed from the literals.
     */
    @Nonnull
    public JsonPath parse(String literal) {
        if (!(literal.startsWith("$.") || literal.startsWith("$["))) {
            throw new IllegalArgumentException("Invalid jsonpath expression. JSONPath must start with a root node identifier and contain at least one segment.");
        }
        if (literal.contains("'") || literal.contains("\\")) {
            throw new IllegalArgumentException("Invalid jsonpath expression. Escape characters are not supported.");
        }
        if (literal.contains("..")) {
            throw new IllegalArgumentException("Invalid jsonpath expression. Descendant segments are not supported.");
        }
        List<String> segments = parseSegments(literal);
        segments.forEach(this::validateSegment);
        return new JsonPath(segments.toArray(String[]::new));
    }

    /**
     * Parses an input literal into a {@link dev.blaauwendraad.masker.json.path.JsonPath} object.
     * Returns null when the input literal does not follow the jsonpath specification.
     *
     * @param literal a jsonpath literal to be parsed.
     * @return a {@link dev.blaauwendraad.masker.json.path.JsonPath} object parsed from the literal.
     */
    public JsonPath tryParse(String literal) {
        try {
            return parse(literal);
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    private List<String> parseSegments(String literal) {
        List<String> segments = new ArrayList<>();
        segments.add("$");
        StringBuilder segment = new StringBuilder();
        for (int i = 2; i < literal.length() - 1; i++) {
            char symbol = literal.charAt(i);
            char nextSymbol = literal.charAt(i + 1);
            if (symbol == '.' || (symbol == '[' && !segment.isEmpty())) {
                segments.add(segment.toString());
                segment = new StringBuilder();
            } else if ((symbol == ']' && nextSymbol == '.') || (symbol == ']' && nextSymbol == '[')) {
                segments.add(segment.toString());
                segment = new StringBuilder();
                i++; // NOSONAR this statement skips the next segment delimiter symbol
            } else if (symbol != '[') {
                segment.append(symbol);
            }
        }
        if (literal.charAt(literal.length() - 1) != ']' && literal.charAt(literal.length() - 1) != '.') {
            segment.append(literal.charAt(literal.length() - 1));
        }
        if (!segment.isEmpty() || literal.endsWith("[]")) {
            segments.add(segment.toString());
        }
        return segments;
    }

    private void validateSegment(String segment) {
        if (isNumber(segment)) {
            throw new IllegalArgumentException("Invalid jsonpath expression. Numbers as key names are not supported.");
        } else if (segment.startsWith("?")) {
            throw new IllegalArgumentException("Invalid jsonpath expression. Filter selectors are not supported.");
        } else if (segment.contains(":")) {
            throw new IllegalArgumentException("Invalid jsonpath expression. Array slice selectors are not supported.");
        } else if (segment.contains("(")) {
            throw new IllegalArgumentException("Invalid jsonpath expression. Function extensions are not supported.");
        }
    }

    private boolean isNumber(String segment) {
        try {
            Long.parseLong(segment);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /**
     * Validates if the input set of json path queries is ambiguous. Throws {@code java.lang.IllegalArgumentException#IllegalArgumentException} if it is.
     * <p>
     * The method does a lexical sort of input jsonpath queries, iterates over sorted values and checks if any local pair is ambiguous.
     * @param jsonPaths input set of jsonpath queries
     */
    public void checkAmbiguity(Set<JsonPath> jsonPaths) {
        List<JsonPath> jsonPathList = jsonPaths.stream().sorted(Comparator.comparing(JsonPath::toString)).toList();
        for (int i = 1; i < jsonPathList.size(); i++) {
            JsonPath current = jsonPathList.get(i - 1);
            JsonPath next = jsonPathList.get(i);
            for (int j = 0; j < Math.min(current.segments().length, next.segments().length); j++) {
                if (!current.segments()[j].equals(next.segments()[j])) {
                    if (current.segments()[j].equals("*") || next.segments()[j].equals("*")) {
                        throw new IllegalArgumentException(String.format("Ambiguous jsonpath keys. '%s' and '%s' combination is not supported.", current, next));
                    }
                    break;
                }
            }
        }

    }

}
