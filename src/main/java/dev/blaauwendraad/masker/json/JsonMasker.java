package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskerAlgorithmType;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.Set;

interface JsonMasker {
    @Nonnull
    static JsonMasker getMasker(String targetKey) {
        return getMasker(JsonMaskingConfig.getDefault(Set.of(targetKey)));
    }

    @Nonnull
    static JsonMasker getMasker(Set<String> targetKeys) {
        return getMasker(JsonMaskingConfig.getDefault(targetKeys));
    }

    @Nonnull
    static JsonMasker getMasker(JsonMaskingConfig maskingConfig) {
        if (maskingConfig.getAlgorithmType() == JsonMaskerAlgorithmType.SINGLE_TARGET_LOOP) {
            return new SingleTargetMasker(maskingConfig);
        } else if (maskingConfig.getAlgorithmType() == JsonMaskerAlgorithmType.KEYS_CONTAIN) {
            return new KeyContainsMasker(maskingConfig);
        } else {
            return new PathAwareKeyContainsMasker(maskingConfig);
        }
    }

    byte[] mask(byte[] input);

    @Nonnull
    default String mask(String input) {
        return new String(mask(input.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }
}
