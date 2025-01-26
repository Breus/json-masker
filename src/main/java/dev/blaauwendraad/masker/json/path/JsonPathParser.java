package dev.blaauwendraad.masker.json.path;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parses a jsonpath literal into a {@link dev.blaauwendraad.masker.json.path.JsonPath} object.
 * <p>
 * The following features from jsonpath specification are not supported:
 * <ul>
 *  <li>Descendant segments</li>
 *  <li>Child segments</li>
 *  <li>Name selectors</li>
 *  <li>Array slice selectors</li>
 *  <li>Index selectors</li>
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

    private static final String ERROR_PREFIX = "Invalid jsonpath expression '%s'. ";

    /**
     * Parses an input literal into a {@link dev.blaauwendraad.masker.json.path.JsonPath} object.
     * Throws {@link java.lang.IllegalArgumentException} when the input literal does not follow the jsonpath specification.
     *
     * @param literal a jsonpath literal to be parsed.
     * @return {@link dev.blaauwendraad.masker.json.path.JsonPath} object parsed from the literal.
     */
    public JsonPath parse(String literal) {
        if (!(literal.equals("$") || literal.startsWith("$.") || literal.startsWith("$["))) {
            throw new IllegalArgumentException(String.format(ERROR_PREFIX, literal) + "JSONPath must start with a root node identifier.");
        }
        if (literal.contains("'") || literal.contains("\\")) {
            throw new IllegalArgumentException(String.format(ERROR_PREFIX, literal) + "Escape characters are not supported.");
        }
        if (literal.contains("..")) {
            throw new IllegalArgumentException(String.format(ERROR_PREFIX, literal) + "Descendant segments are not supported.");
        }
        List<String> segments = parseSegments(literal);
        segments.forEach(segment -> validateSegment(segment, literal));
        return new JsonPath(segments.toArray(String[]::new));
    }

    /**
     * Parses an input literal into a {@link dev.blaauwendraad.masker.json.path.JsonPath} object.
     * Returns null when the input literal does not follow the jsonpath specification.
     *
     * @param literal a jsonpath literal to be parsed.
     * @return a {@link dev.blaauwendraad.masker.json.path.JsonPath} object parsed from the literal.
     */
    @Nullable
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
        if (literal.equals("$")) {
            return segments;
        }
        StringBuilder segment = new StringBuilder();
        for (int i = 2; i < literal.length() - 1; i++) {
            char symbol = literal.charAt(i);
            char nextSymbol = literal.charAt(i + 1);
            if (symbol == '.' || (symbol == '[' && segment.length() != 0)) {
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
        if (segment.length() != 0 || literal.endsWith("[]")) {
            segments.add(segment.toString());
        }
        if (segments.size() > 1 && segments.get(segments.size() - 1).equals("*") && !segments.get(segments.size() - 2).equals("*")) {
            throw new IllegalArgumentException(String.format(ERROR_PREFIX, literal) + "A single leading wildcard is not allowed. " +
                    "Use '" + literal.substring(0, literal.length() - 2) + "' instead.");

        }
        return segments;
    }

    private void validateSegment(String segment, String literal) {
        if (isNumber(segment)) {
            throw new IllegalArgumentException(String.format(ERROR_PREFIX, literal) + "Numbers as key names are not supported.");
        } else if (segment.startsWith("?")) {
            throw new IllegalArgumentException(String.format(ERROR_PREFIX, literal) + "Filter selectors are not supported.");
        } else if (segment.contains(":")) {
            throw new IllegalArgumentException(String.format(ERROR_PREFIX, literal) + "Array slice selectors are not supported.");
        } else if (segment.contains("(")) {
            throw new IllegalArgumentException(String.format(ERROR_PREFIX, literal) + "Function extensions are not supported.");
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
     * Validates if the input set of JSONPath queries contains ambiguous segments. Throws {@code java.lang.IllegalArgumentException#IllegalArgumentException} if it does.
     * <p>
     * The method does a lexical sort of input jsonpath queries, iterates over sorted values and checks if any local pair is ambiguous.
     *
     * @param jsonPaths input set of jsonpath queries
     */
    public void checkAmbiguity(Set<JsonPath> jsonPaths) {
        List<JsonPath> jsonPathList = jsonPaths.stream().sorted(Comparator.comparing(JsonPath::toString)).collect(Collectors.toUnmodifiableList());
        for (int i = 1; i < jsonPathList.size(); i++) {
            JsonPath current = jsonPathList.get(i - 1);
            JsonPath next = jsonPathList.get(i);
            for (int j = 0; j < current.segments().length; j++) {
                if (!current.segments()[j].equals(next.segments()[j])) {
                    if (current.segments()[j].equals("*") || next.segments()[j].equals("*")) {
                        String commonPath = String.join(".", Arrays.copyOfRange(current.segments(), 0, j));
                        throw new IllegalArgumentException(String.format("'%s' and '%s' JSONPath keys combination is not supported: ambiguity at segment %d with shared path %s.", current, next, j, commonPath));
                    }
                    break;
                }
            }
        }
    }

}
