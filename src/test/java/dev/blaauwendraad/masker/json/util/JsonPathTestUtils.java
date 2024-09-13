package dev.blaauwendraad.masker.json.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.blaauwendraad.masker.json.path.JsonPathParser;
import dev.blaauwendraad.masker.randomgen.RandomJsonGenerator;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class JsonPathTestUtils {

    /**
     * Transforms an input set of keys into a set of JSONPath keys for a given json.
     * The input set of keys are assumed not to be jsonpath keys already.
     * If a key from the input set does not exist in the json,
     * then a path from the down-left most key in the json is used as a prefix to the key.
     * <p>
     * Given that the input will expand to much larger set of JSONPaths, which would lead to skewed benchmarks,
     * only the subset of JSONPaths is returned, that is equal to amount of incoming keys.
     *
     * @param keys a set of keys to be transformed into jason path keys. Assumed not to be jsonpath keys already.
     * @param json a target json.
     * @return a set of JSONPath keys transformed from <code>keys</code>.
     */
    public static Set<String> transformToJsonPathKeys(Set<String> keys, String json) {
        JsonNode root;
        try {
            root = new ObjectMapper().readTree(json.toLowerCase());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Illegal input json.", e);
        }
        JsonPathParser jsonPathParser = new JsonPathParser();
        Set<String> transformedTargetKeys = new HashSet<>();
        for (String targetKey : keys) {
            targetKey = targetKey.toLowerCase();
            Deque<Map.Entry<String, JsonNode>> stack = new ArrayDeque<>();
            stack.push(new AbstractMap.SimpleEntry<>("$", root));
            while (!stack.isEmpty()) {
                Map.Entry<String, JsonNode> curr = stack.pop();
                String candidateKey = curr.getKey() + "." + targetKey;
                if (jsonPathParser.tryParse(candidateKey) == null) {
                    continue;
                }
                if (curr.getValue() instanceof ObjectNode currObject) {
                    Iterator<Map.Entry<String, JsonNode>> fieldsIterator = currObject.fields();
                    while (fieldsIterator.hasNext()) {
                        Map.Entry<String, JsonNode> child = fieldsIterator.next();
                        if (child.getKey().equals(targetKey)) {
                            transformedTargetKeys.add(candidateKey);
                        }
                        stack.push(new AbstractMap.SimpleEntry<>(curr.getKey() + "." + child.getKey(), child.getValue()));
                    }
                } else if (curr.getValue() instanceof ArrayNode currArray) {
                    for (int i = 0; i < currArray.size(); i++) {
                        stack.push(new AbstractMap.SimpleEntry<>(curr.getKey() + "[*]", currArray.get(i)));
                    }
                }
                if (stack.isEmpty()) {
                    // the key does not exist
                    transformedTargetKeys.add(candidateKey);
                }
            }
        }
        List<String> allKeys = disambiguate(new ArrayList<>(transformedTargetKeys));
        Collections.shuffle(allKeys, new Random(RandomJsonGenerator.STATIC_RANDOM_SEED));
        return new HashSet<>(allKeys.subList(0, Math.min(keys.size(), allKeys.size())));
    }

    /**
     * Disambiguates the input list of jsonpath keys.
     * <p>
     * If two keys <code>$.a.b</code> and <code>$.a</code> are present, the more specific <code>$.a.b</code> will be chosen.
     *
     * @param jsonPathKeys the input list of jsonpath keys
     * @return disambiguated set of jsonpath keys
     */
    public static List<String> disambiguate(List<String> jsonPathKeys) {
        Set<String> disambiguated = new HashSet<>(jsonPathKeys);
        Collections.sort(jsonPathKeys);
        for (int i = 1; i < jsonPathKeys.size(); i++) {
            String current = jsonPathKeys.get(i-1);
            String next = jsonPathKeys.get(i);
            if (next.indexOf(current) == 0 && !next.equals(current) && next.charAt(current.length()) == '.') {
                disambiguated.remove(current);
            }
        }
        return new ArrayList<>(disambiguated);
    }
}
