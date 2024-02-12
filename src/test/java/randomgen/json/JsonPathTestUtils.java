package randomgen.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class JsonPathTestUtils {

    /**
     * Transforms an input set of keys into a set of json path keys for a given json.
     * The input set of keys are assumed not to be jsonpath keys already.
     * If a key from the input set does not exist in the json,
     * then a path from the down-left most key in the json is used as a prefix to the key
     *
     * @param keys a set of keys to be transformed into jason path keys. Assumed not to be jsonpath keys already.
     * @param json a target json.
     * @return a set of json path keys transformed from <code>keys</code>.
     */
    public static Set<String> transformToJsonPathKeys(Set<String> keys, String json) {
        JsonNode root;
        try {
            root = new ObjectMapper().readTree(json.toLowerCase());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Illegal input json.");
        }
        Set<String> transformedTargetKeys = new HashSet<>();
        for (String targetKey : keys) {
            targetKey = targetKey.toLowerCase();
            Deque<Map.Entry<String, JsonNode>> stack = new ArrayDeque<>();
            stack.push(new AbstractMap.SimpleEntry<>("$", root));
            while (!stack.isEmpty()) {
                Map.Entry<String, JsonNode> curr = stack.pop();
                if (curr.getValue() instanceof ObjectNode currObject) {
                    Iterator<Map.Entry<String, JsonNode>> fieldsIterator = currObject.fields();
                    while (fieldsIterator.hasNext()) {
                        Map.Entry<String, JsonNode> child = fieldsIterator.next();
                        if (child.getKey().equals(targetKey)) {
                            transformedTargetKeys.add(curr.getKey() + "." + targetKey);
                        }
                        stack.push(new AbstractMap.SimpleEntry<>(curr.getKey() + "." + child.getKey(), child.getValue()));
                    }
                } else if (curr.getValue() instanceof ArrayNode currArray) {
                    for (int i = 0; i < currArray.size(); i++) {
                        stack.push(new AbstractMap.SimpleEntry<>(curr.getKey() + "[" + i + "]", currArray.get(i)));
                    }
                }
                if (stack.isEmpty()) {
                    // the key does not exist
                    transformedTargetKeys.add(curr.getKey() + "." + targetKey);
                }
            }
        }
        return transformedTargetKeys;
    }
}
