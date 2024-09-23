package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.blaauwendraad.masker.randomgen.RandomJsonGenerator;
import dev.blaauwendraad.masker.randomgen.RandomJsonGeneratorConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
                .setRandomSeed(RandomJsonGenerator.STATIC_RANDOM_SEED)
                .createConfig();

        return new RandomJsonGenerator(config).createRandomJsonNode().toString();
    }
}
