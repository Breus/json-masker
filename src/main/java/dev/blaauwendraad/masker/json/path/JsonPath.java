package dev.blaauwendraad.masker.json.path;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
 * <p>
 * JsonPath can contain empty keys.
 * For example, "$.a..b." denotes the path to the "target" value in the following json: {"a":{"":{"b":{"":"target"}}}}
 */
public class JsonPath {
    private final String[] pathComponents;

    JsonPath(String[] pathComponents) {
        this.pathComponents = pathComponents;
    }

    @Nonnull
    public static JsonPath from(String jsonPathLiteral) {
        if (!jsonPathLiteral.startsWith("$")) {
            throw new IllegalArgumentException("JSONPath literal must start with a \"$\"");
        }
        List<String> components = new ArrayList<>();
        StringBuilder component = new StringBuilder();
        for (char ch : jsonPathLiteral.toCharArray()) {
            if (ch == '.') {
                components.add(component.toString());
                component = new StringBuilder();
            } else {
                component.append(ch);
            }
        }
        components.add(component.toString());
        return new JsonPath(components.toArray(String[]::new));
    }

    public String[] getPathComponents() {
        return pathComponents;
    }

    public String getLastComponent() {
        return pathComponents[pathComponents.length-1];
    }

    @Override
    public String toString() {
        return String.join(".", pathComponents);
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
