package dev.blaauwendraad.masker.json.path;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * A {@link JsonPath} expression is used to traverse, select and extract fields and values from a JSON document.
 * Example JSON and corresponding paths:
 * <pre>
 * {
 *     "a": "1",
 *     "b": {
 *         "c": "2"
 *     },
 *     "d": [
 *          {
 *              "e": "3",
 *              "f": "4"
 *          },
 *          {
 *              "g": "5"
 *          }
 *     ]
 * }
 * </pre>
 * In the context of this library, any JsonPath starts with a '$' symbol, which denotes the root node.
 * Then, access to nested JSON objects is denoted by a dot ('.') followed by a String name
 * selector which denotes the key of the nested object.
 * Furthermore, a numeric index selector (e.g. 3) selects an indexed child of an array.
 * So, considering the example JSON above:
 * <ul>
 * <li> $.a = "1" </li>
 * <li> $.b.c = "2" </li>
 * <li> $.d[0].e = "3" </li>
 * <li> $.d[0].f = "4" </li>
 * <li> $.d[1].g = "5" </li>
 * </ul>
 * These are all the parts of JSONPath considered for this library, to be able to denote a target key matching one and
 * only one value in the JSON document to be masked. This is useful in cases where the same JSON key appears multiple
 * times in the same JSON document (i.e. in different objects).
 *
 */
public class JsonPath {
    private final String[] pathComponents;

    JsonPath(String[] pathComponents) {
        this.pathComponents = pathComponents;
    }

    @Nonnull
    public static JsonPath from(String jsonPathLiteral) {
        if (!jsonPathLiteral.startsWith("$.")) {
            throw new IllegalArgumentException("JSONPath literal must start with a \"$.\"");
        }
        if (jsonPathLiteral.length() < 3) {
            throw new IllegalArgumentException("JSONPath must contain at least one name selector");
        }
        if (jsonPathLiteral.charAt(jsonPathLiteral.length() - 1) == '.') {
            throw new IllegalArgumentException("JSONPath cannot end with a component separator ('.')");
        }
        var jsonPathLiteralTmp = jsonPathLiteral.substring(2);
        return new JsonPath(jsonPathLiteralTmp.split("\\."));
    }

    public String[] getPathComponents() {
        return pathComponents;
    }

    @Override
    public String toString() {
        return "$." + String.join(".", pathComponents);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JsonPath jsonPath = (JsonPath) o;
        return Arrays.equals(pathComponents, jsonPath.pathComponents);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(pathComponents);
    }
}
