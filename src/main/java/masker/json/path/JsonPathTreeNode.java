package masker.json.path;

import masker.json.PathAwareKeyContainsMasker;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Tree representation of (a set of) {@link JsonPath}'s for constant-time lookup in the {@link PathAwareKeyContainsMasker}.
 */
public class JsonPathTreeNode {
    private final Map<String, JsonPathTreeNode> children;
    private boolean isLeafNode;

    public JsonPathTreeNode() {
        this.children = new HashMap<>();
        this.isLeafNode = false;
    }

    public JsonPathTreeNode(boolean isLeafNode) {
        this.children = new HashMap<>();
        this.isLeafNode = isLeafNode;
    }

    public JsonPathTreeNode(Map<String, JsonPathTreeNode> children, boolean isLeafNode) {
        this.children = children;
        this.isLeafNode = isLeafNode;
    }

    @NotNull
    public static JsonPathTreeNode of(@NotNull Set<JsonPath> jsonPaths) {
        JsonPathTreeNode root = new JsonPathTreeNode();
        int depth = 0;
        while (true) {
            boolean found = false;
            for (JsonPath jsonPath : jsonPaths) {
                if (jsonPath.getPathComponents().length > depth) {
                    JsonPathTreeNode parent = resolveParent(root, jsonPath.getPathComponents(), depth);
                    JsonPathTreeNode node = parent.children.computeIfAbsent(
                            jsonPath.getPathComponents()[depth],
                            key -> new JsonPathTreeNode()
                    );
                    node.isLeafNode = node.isLeafNode || jsonPath.getPathComponents().length == depth + 1;
                    found = true;
                }
            }
            depth++;
            if (!found) {
                break;
            }
        }
        return root;
    }

    private static JsonPathTreeNode resolveParent(JsonPathTreeNode root, String[] pathComponents, int depth) {
        for (int i = 0; i < depth;  i++) {
            root = root.children.get(pathComponents[i]);
        }
        return root;
    }

    public Map<String, JsonPathTreeNode> getChildren() {
        return children;
    }

    public boolean isLeafNode() {
        return isLeafNode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JsonPathTreeNode that = (JsonPathTreeNode) o;
        return isLeafNode == that.isLeafNode && Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(children, isLeafNode);
    }
}

