package masker.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import masker.json.config.JsonMaskerAlgorithmType;
import masker.json.config.JsonMaskingConfig;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class JsonMaskerTestUtil {
    private JsonMaskerTestUtil() {
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<JsonMaskerTestInstance> getJsonMaskerTestInstancesFromFile(
            String fileName,
            Set<JsonMaskerAlgorithmType> algorithmTypes
    ) throws IOException {
        List<JsonMaskerTestInstance> testInstances = new ArrayList<>();
        ArrayNode jsonArray =
                mapper.readValue(
                        JsonMaskerTestUtil.class.getClassLoader().getResource(fileName),
                        ArrayNode.class
                );
        var reader =
                mapper.readerFor(TypeFactory.defaultInstance().constructCollectionType(Set.class, String.class));
        for (JsonNode jsonNode : jsonArray) {
            JsonMaskingConfig.Builder configBuilder = JsonMaskingConfig.custom(reader.readValue(jsonNode.get(
                    "targetKeys")));
            JsonNode maskerConfig = jsonNode.findValue("maskerConfig");
            if (maskerConfig != null) {
                JsonNode obfuscationLength = maskerConfig.findValue("obfuscationLength");
                if (obfuscationLength != null) {
                    configBuilder.obfuscationLength(obfuscationLength.asInt());
                }
                JsonNode maskNumberValuesWith = maskerConfig.findValue("maskNumberValuesWith");
                if (maskNumberValuesWith != null) {
                    configBuilder.maskNumberValuesWith(maskNumberValuesWith.asInt());
                }
                JsonNode caseSensitiveTargetKeys = maskerConfig.findValue("caseSensitiveTargetKeys");
                if (caseSensitiveTargetKeys != null && caseSensitiveTargetKeys.booleanValue()) {
                    configBuilder.caseSensitiveTargetKeys();
                }
            }
            JsonMaskingConfig maskingConfig = configBuilder.build();
            var input = jsonNode.get("input").toString();
            if (jsonNode.get("input").isTextual() && jsonNode.get("input").textValue().startsWith("file://")) {
                URL resourceUrl = JsonMaskerTestUtil.class.getClassLoader().getResource(jsonNode.get("input").textValue().replace("file://", ""));
                try {
                    input = Files.readString(Path.of(Objects.requireNonNull(resourceUrl).toURI()));
                } catch (URISyntaxException e) {
                    throw new IOException("Cannot read file " + resourceUrl, e);
                }
            }
            var expectedOutput = jsonNode.get("expectedOutput").toString();
            if (jsonNode.get("expectedOutput").isTextual() && jsonNode.get("expectedOutput").textValue().startsWith("file://")) {
                URL resourceUrl = JsonMaskerTestUtil.class.getClassLoader().getResource(jsonNode.get("expectedOutput").textValue().replace("file://", ""));
                try {
                    expectedOutput = Files.readString(Path.of(Objects.requireNonNull(resourceUrl).toURI()));
                } catch (URISyntaxException e) {
                    throw new IOException("Cannot read file " + resourceUrl, e);
                }
            }
            if (algorithmTypes.contains(JsonMaskerAlgorithmType.KEYS_CONTAIN)) {
                testInstances.add(new JsonMaskerTestInstance(
                        input,
                        expectedOutput,
                        new KeyContainsMasker(maskingConfig)
                ));
            }
            if (algorithmTypes.contains(JsonMaskerAlgorithmType.SINGLE_TARGET_LOOP)) {
                testInstances.add(new JsonMaskerTestInstance(
                        input,
                        expectedOutput,
                        new SingleTargetMasker(maskingConfig)
                ));
            }
//            if (algorithmTypes.contains(JsonMaskerAlgorithmType.PATH_AWARE_KEYS_CONTAIN)) {
//                testInstances.add(new JsonMaskerTestInstance(
//                        input,
//                        expectedOutput,
//                        new PathAwareKeyContainsMasker(maskingConfig)
//                ));
//            }
        }
        return testInstances;
    }
}
