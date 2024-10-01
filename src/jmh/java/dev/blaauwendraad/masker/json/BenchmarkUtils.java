package dev.blaauwendraad.masker.json;

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

    public static final long STATIC_RANDOM_SEED = 1285756302517652226L;
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

    public static JsonNode randomJson(Set<String> targetKeys, String jsonSize, String characters, double targetKeyPercentage) {
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
                .setRandomSeed(STATIC_RANDOM_SEED)
                .createConfig();

        JsonNode jsonNode = new RandomJsonGenerator(config).createRandomJsonNode();
        Set<String> jsonPaths = new HashSet<>();
        Set<String> lowerCaseTargetKeys = targetKeys.stream().map(String::toLowerCase).collect(Collectors.toSet());
        collectJsonPaths(jsonNode, jsonPaths, "$", lowerCaseTargetKeys);
        return jsonNode;
    }

    private static void collectJsonPaths(JsonNode jsonNode, Set<String> jsonPaths, String currentJsonPath, Set<String> targetKeys) {
        if (jsonNode instanceof ObjectNode objectNode) {
            Iterable<String> fieldNames = objectNode::fieldNames;
            for (String fieldName : fieldNames) {
                if (fieldName.isEmpty() || fieldName.startsWith(".") || fieldName.endsWith(".") || fieldName.contains("..")) {
                    // such filed would result in descendant segment '..' that we don't support
                    continue;
                }
                String jsonPathKey = currentJsonPath + "." + fieldName;
                if (targetKeys.contains(fieldName.toLowerCase())) {
                    jsonPaths.add(jsonPathKey);
                }
                collectJsonPaths(jsonNode.get(fieldName), jsonPaths, jsonPathKey, targetKeys);
            }
        } else if (jsonNode instanceof ArrayNode arrayNode) {
            String jsonPathKey = currentJsonPath + "[*]";
            for (int i = 0; i < arrayNode.size(); i++) {
                collectJsonPaths(arrayNode.get(i), jsonPaths, jsonPathKey, targetKeys);
            }
        }
    }

    public static Set<String> collectJsonPaths(JsonNode jsonNode, Set<String> targetKeys) {
        Set<String> jsonPaths = new HashSet<>();
        Set<String> lowerCaseTargetKeys = targetKeys.stream().map(String::toLowerCase).collect(Collectors.toSet());
        collectJsonPaths(jsonNode, jsonPaths, "$", lowerCaseTargetKeys);
        return disambiguate(jsonPaths);
    }

    /**
     * Disambiguates the input list of jsonpath keys.
     * <p>
     * If two keys <code>$.a.b</code> and <code>$.a</code> are present, the more specific <code>$.a.b</code> will be chosen.
     *
     * @param jsonPaths the input list of jsonpath keys
     * @return disambiguated set of jsonpath keys
     */
    public static Set<String> disambiguate(Set<String> jsonPaths) {
        List<String> jsonPathKeys = new ArrayList<>(jsonPaths);
        Collections.sort(jsonPathKeys);
        JsonPathParser jsonPathParser = new JsonPathParser();
        for (int i = 1; i < jsonPathKeys.size(); i++) {
            String current = jsonPathKeys.get(i - 1);
            String next = jsonPathKeys.get(i);
            try {
                jsonPathParser.checkAmbiguity(Set.of(jsonPathParser.parse(current), jsonPathParser.parse(next)));
            } catch (IllegalArgumentException ignore) {
                jsonPathKeys.remove(next);
                i--;
            }
        }
        return new HashSet<>(jsonPathKeys);
    }
}
