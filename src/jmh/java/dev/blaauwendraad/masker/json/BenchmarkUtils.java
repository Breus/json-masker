package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.blaauwendraad.masker.json.path.JsonPathParser;
import dev.blaauwendraad.masker.randomgen.RandomJsonGenerator;
import dev.blaauwendraad.masker.randomgen.RandomJsonGeneratorConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static dev.blaauwendraad.masker.json.util.JsonStringCharacters.getPrintableAsciiCharacters;
import static dev.blaauwendraad.masker.json.util.JsonStringCharacters.getRandomPrintableUnicodeCharacters;
import static dev.blaauwendraad.masker.json.util.JsonStringCharacters.getUnicodeControlCharacters;
import static dev.blaauwendraad.masker.json.util.JsonStringCharacters.mergeCharSets;

public class BenchmarkUtils {

    public static final int TARGET_KEYS_SIZE = 2385;
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static List<String> loadSampleTargetKeys() {
        try {
            URL targetKeyFileUrl = RandomJsonGenerator.class.getResource("/target_keys.json");
            List<String> targetKeyList = new ArrayList<>();
            BenchmarkUtils.OBJECT_MAPPER.readValue(targetKeyFileUrl, ArrayNode.class).forEach(t -> targetKeyList.add(t.textValue()));

            if (targetKeyList.size() != BenchmarkUtils.TARGET_KEYS_SIZE) {
                throw new IllegalArgumentException("Number of keys does not match, please adjust the constant");
            }
            return targetKeyList;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static int parseSize(String size) {
        // use regex to parse the jsonSize param
        Pattern pattern = Pattern.compile("^(\\d+)(\\w+)$");
        var matcher = pattern.matcher(size);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid size param: " + size);
        }
        int sizeBytes = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2).toLowerCase();
        switch (unit) {
            case "kb":
                sizeBytes *= 1024;
                break;
            case "mb":
                sizeBytes *= 1024 * 1024;
                break;
            default:
                throw new IllegalArgumentException("Invalid size param: " + size);
        }
        return sizeBytes;
    }

    public static Set<String> getTargetKeys(int count) {
        Set<String> targetKeys = new HashSet<>();
        for (int i = 0; i < count; i++) {
            targetKeys.add("someSecret" + i);
        }
        return targetKeys;
    }

    public static String randomJson(Set<String> targetKeys, String jsonSize, String characters, double targetKeyPercentage) {
        Set<Character> allowedCharacters = switch (characters) {
            case "ascii (no quote)" -> getPrintableAsciiCharacters()
                        .stream()
                        .filter(c -> c != '"')
                        .collect(Collectors.toSet());
            case "ascii" -> getPrintableAsciiCharacters();
            case "unicode" -> mergeCharSets(
                    getPrintableAsciiCharacters(),
                    getUnicodeControlCharacters(),
                    getRandomPrintableUnicodeCharacters()
            );
            default -> throw new IllegalArgumentException("Invalid characters param: " + characters + ", must be one of: 'ascii (no quote)', 'ascii', 'unicode'");
        };
        RandomJsonGeneratorConfig config = RandomJsonGeneratorConfig.builder()
                .setAllowedCharacters(allowedCharacters)
                .setTargetKeys(targetKeys)
                .setTargetKeyPercentage(targetKeyPercentage)
                .setTargetJsonSizeBytes(BenchmarkUtils.parseSize(jsonSize))
                .setRandomSeed(1285756302517652226L)
                .createConfig();

        return new RandomJsonGenerator(config).createRandomJsonNode().toString();
    }

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
        Collections.shuffle(allKeys, new Random(1285756302517652226L));
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
