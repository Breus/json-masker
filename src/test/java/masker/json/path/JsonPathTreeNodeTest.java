package masker.json.path;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class JsonPathTreeNodeTest {

    @ParameterizedTest
    @MethodSource("testCases")
    public void of_SetsOfJsonPaths_ReturnsExpectedTree(Set<JsonPath> jsonPaths, JsonPathTreeNode expected) {
        JsonPathTreeNode result = JsonPathTreeNode.of(jsonPaths);
        assertNodesEqual(expected, result);
    }

    private void assertNodesEqual(JsonPathTreeNode expected, JsonPathTreeNode actual) {
        assertEquals(expected.getChildren().size(), actual.getChildren().size());
        assertEquals(expected.isLeafNode(), actual.isLeafNode());

        for (String key : expected.getChildren().keySet()) {
            assertTrue(actual.getChildren().containsKey(key));
            JsonPathTreeNode expectedChild = expected.getChildren().get(key);
            JsonPathTreeNode actualChild = actual.getChildren().get(key);
            assertNodesEqual(expectedChild, actualChild);
        }
    }

    static Stream<Arguments> testCases() {
        return Stream.of(
                Arguments.of(
                        Set.of(JsonPath.from("$.a.b.c"), JsonPath.from("$.a.d.e")),
                        new JsonPathTreeNode(
                                Map.of(
                                        "a", new JsonPathTreeNode(
                                                Map.of(
                                                        "b", new JsonPathTreeNode(
                                                                Map.of(
                                                                        "c", new JsonPathTreeNode(true)
                                                                ),
                                                                false
                                                        ),
                                                        "d", new JsonPathTreeNode(
                                                                Map.of(
                                                                        "e", new JsonPathTreeNode(true)
                                                                ),
                                                                false
                                                        )
                                                ),
                                                false
                                        )
                                ),
                                false
                        )
                ),
                Arguments.of(
                        Set.of(JsonPath.from("$.a.b.c"), JsonPath.from("$.a.b.d")),
                        new JsonPathTreeNode(
                                Map.of(
                                        "a", new JsonPathTreeNode(
                                                Map.of(
                                                        "b", new JsonPathTreeNode(
                                                                Map.of(
                                                                        "c", new JsonPathTreeNode(true),
                                                                        "d", new JsonPathTreeNode(true)
                                                                ),
                                                                false
                                                        )
                                                ),
                                                false
                                        )
                                ),
                                false
                        )
                ),
                Arguments.of(
                        Set.of(JsonPath.from("$.a.b.c")),
                        new JsonPathTreeNode(
                                Map.of(
                                        "a", new JsonPathTreeNode(
                                                Map.of(
                                                        "b", new JsonPathTreeNode(
                                                                Map.of(
                                                                        "c", new JsonPathTreeNode(true)
                                                                ),
                                                                false
                                                        )
                                                ),
                                                false
                                        )
                                ),
                                false
                        )
                ),
                Arguments.of(
                        Set.of(
                                JsonPath.from("$.a.b.c"),
                                JsonPath.from("$.a.b.d"),
                                JsonPath.from("$.a.b")
                        ),
                        new JsonPathTreeNode(
                                Map.of(
                                        "a", new JsonPathTreeNode(
                                                Map.of(
                                                        "b", new JsonPathTreeNode(
                                                                Map.of(
                                                                        "c", new JsonPathTreeNode(true),
                                                                        "d", new JsonPathTreeNode(true)
                                                                ),
                                                                true
                                                        )
                                                ),
                                                false
                                        )
                                ),
                                false
                        )
                )
        );
    }
}