package dev.blaauwendraad.masker.json.path;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a jsonpath literal into a {@link dev.blaauwendraad.masker.json.path.JsonPath} object.
 * <p>
 * The following features from jsonpath specification are not supported:
 * <ul>
 *  <li>Wildcard segments</li>
 *  <li>Descendant segments</li>
 *  <li>Wildcard selectors</li>
 *  <li>Array slice selectors</li>
 *  <li>Filter selectors</li>
 *  <li>Function extensions</li>
 *  <li>Escape characters</li>
 * </ul>
 */
public class JsonPathParser {

    @Nonnull
    public JsonPath parse(String literal) {
        if (!literal.startsWith("$")) {
            throw new IllegalArgumentException("Illegal jsonpath literal. JSONPath must start with a root node identifier \"$\".");
        }
        if (literal.length() <= 2) {
            throw new IllegalArgumentException("Illegal jsonpath literal. JSONPath must contain at least one segment.");
        }
        if (literal.contains("'") || literal.contains("\\")) {
            throw new IllegalArgumentException("Illegal jsonpath literal. Escape characters are not supported.");
        }
        if (literal.contains("..")) {
            throw new IllegalArgumentException("Illegal jsonpath literal. Descendant segments are not supported.");
        }
        List<String> segments = parseSegments(literal);
        segments.forEach(this::validateSegment);
        return new JsonPath(segments.toArray(String[]::new));
    }

    public JsonPath tryParse(String literal) {
        JsonPath jsonPath = null;
        try {
            jsonPath = parse(literal);
        } catch (IllegalArgumentException ignore) {}
        return jsonPath;
    }

    private List<String> parseSegments(String literal) {
        List<String> segments = new ArrayList<>();
        segments.add("$");
        StringBuilder segment = new StringBuilder();
        for (int i = 2; i < literal.length()-1; i++) {
            char symbol = literal.charAt(i);
            char nextSymbol = literal.charAt(i + 1);
            if (symbol == '.' || (symbol == '[' && !segment.isEmpty())) {
                segments.add(segment.toString());
                segment = new StringBuilder();
            } else if ((symbol == ']' && nextSymbol == '.') || (symbol == ']' && nextSymbol == '[')) {
                segments.add(segment.toString());
                segment = new StringBuilder();
                i++;
            } else if (symbol != '[') {
                segment.append(symbol);
            }
        }
        if (literal.charAt(literal.length()-1) != ']' && literal.charAt(literal.length()-1) != '.') {
            segment.append(literal.charAt(literal.length()-1));
        }
        if (!segment.isEmpty() || literal.endsWith("[]")) {
            segments.add(segment.toString());
        }
        return segments;
    }

    private void validateSegment(String segment) {
        if (segment.startsWith("*")) {
            throw new IllegalArgumentException("Illegal jsonpath literal. Wildcards are not supported.");
        } else if (segment.startsWith("?")) {
            throw new IllegalArgumentException("Illegal jsonpath literal. Filter selectors are not supported.");
        } else if (segment.contains(":")) {
            throw new IllegalArgumentException("Illegal jsonpath literal. Array slice selectors are not supported.");
        } else if (segment.contains("(")) {
            throw new IllegalArgumentException("Illegal jsonpath literal. Function extensions are not supported.");
        }
    }

}
