package masker.json.path;

import org.jetbrains.annotations.NotNull;

/**
 * A {@link JsonPath} expression is used to traverse, select and extract fields and values from a JSON document.
 * <p>
 * Example JSON and corresponding paths:
 * <p>
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

    public JsonPath(String[] pathComponents) {
        this.pathComponents = pathComponents;
    }

    @NotNull
    public JsonPath toJsonPath(@NotNull String jsonPathLiteral) {
        if(! jsonPathLiteral.startsWith("$")) {
            throw new IllegalArgumentException("JSONPath literal must start with an '$'");
        }
        return new JsonPath(jsonPathLiteral.split("\\."));
    }

    @Override
    public String toString() {
        return "$" + String.join(".", pathComponents);
    }
}
