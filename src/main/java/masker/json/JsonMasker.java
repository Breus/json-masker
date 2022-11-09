package masker.json;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Set;

interface JsonMasker {
    @NotNull
    static JsonMasker getMasker(@NotNull String targetKey) {
        return getMasker(JsonMaskingConfig.getDefault(Set.of(targetKey)));
    }

    @NotNull
    static JsonMasker getMasker(@NotNull Set<String> targetKeys) {
        return getMasker(JsonMaskingConfig.getDefault(targetKeys));
    }

    @NotNull
    static JsonMasker getMasker(@NotNull JsonMaskingConfig maskingConfig) {
        if (maskingConfig.getAlgorithmType() == JsonMaskerAlgorithmType.SINGLE_TARGET_LOOP) {
            return new SingleTargetMasker(maskingConfig);
        } else if (maskingConfig.getAlgorithmType() == JsonMaskerAlgorithmType.KEYS_CONTAIN){
            return new KeyContainsMasker(maskingConfig);
        } else {
            return new PathAwareKeyContainsMasker(maskingConfig);
        }
    }

    byte[] mask(byte[] input);

    @NotNull
    default String mask(@NotNull String input) {
        return new String(mask(input.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }
}
