package dev.blaauwendraad.masker.json.config;

/**
 * Specifies the masking algorithm used.
 * <p>
 * The main reason this library contains this enum is for future support of additional algorithms that might focus on
 * different requirements for which a different algorithm provides a better performance.
 * <p>
 * Default value: {@link JsonMaskerAlgorithmType#KEYS_CONTAIN}
 */
public enum JsonMaskerAlgorithmType {
    KEYS_CONTAIN,
}
