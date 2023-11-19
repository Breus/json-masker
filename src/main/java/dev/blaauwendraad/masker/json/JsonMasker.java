package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskerAlgorithmType;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Masker that can be used to mask JSON objects and arrays.
 */
public interface JsonMasker {
    /**
     * Creates a default {@link JsonMasker} with the provided target key.
     *
     * @param targetKey the key to target
     * @return the {@link JsonMasker} instance
     */
    @Nonnull
    static JsonMasker getMasker(String targetKey) {
        return getMasker(JsonMaskingConfig.getDefault(Set.of(targetKey)));
    }

    /**
     * Creates a default {@link JsonMasker} with the provided target key(s).
     *
     * @param targetKeys the key(s) to target
     * @return the {@link JsonMasker} instance
     */
    @Nonnull
    static JsonMasker getMasker(Set<String> targetKeys) {
        return getMasker(JsonMaskingConfig.getDefault(targetKeys));
    }

    /**
     * Creates a {@link JsonMasker} with the provided {@link JsonMaskingConfig}.
     *
     * @param maskingConfig the JSON masker configuration
     * @return a new {@link JsonMasker} instance corresponding to the provided {@link JsonMaskingConfig}
     */
    @Nonnull
    static JsonMasker getMasker(JsonMaskingConfig maskingConfig) {
        if (maskingConfig.getAlgorithmType() == JsonMaskerAlgorithmType.KEYS_CONTAIN) {
            return new KeyContainsMasker(maskingConfig);
        } else {
            throw new IllegalArgumentException("Unknown masking algorithm type: " + maskingConfig.getAlgorithmType());
        }
    }

    /**
     * Masks the given JSON input and returns the masked output.
     *
     * @param input the JSON input as bytes
     * @return the masked JSON output as bytes
     */
    byte[] mask(byte[] input);

    /**
     * Masks the given JSON input and returns the masked output.
     *
     * @param input the JSON input as String
     * @return the masked JSON output String
     */
    @Nonnull
    default String mask(String input) {
        return new String(mask(input.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }
}
