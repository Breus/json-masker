package masker.json;

import java.util.stream.Stream;

public final class JsonAlgorithmTestUtil {
    static Stream<JsonMasker> getAllJsonMaskerArgs(JsonMaskingConfig jsonMaskingConfig) {
        return Stream.of(
                new KeyContainsMasker(jsonMaskingConfig),
                new SingleTargetMasker(jsonMaskingConfig),
                new PathAwareKeyContainsMasker(jsonMaskingConfig)
        );
    }
}
