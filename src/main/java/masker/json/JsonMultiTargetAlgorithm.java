package masker.json;

/**
 * Specifies the multi target key masking algorithm used to mask multiple be
 * <p>
 * {@link JsonMultiTargetAlgorithm#SINGLE_TARGET_LOOP}   loops over the target key set and executes the single-target key masking algorithm for each key.
 * (time complexity cN * M, where N is the message input size, M the target key set size, and c is some constant)
 * {@link JsonMultiTargetAlgorithm#KEYS_CONTAIN}         uses a dedicated multi-target algorithm by looking for a JSON key and checking whether the target key set contains this key.
 * (time complexity cN, where N is the message input size and c is some constant)
 * Note: for small target key set (1-2 entries), the {@link JsonMultiTargetAlgorithm#SINGLE_TARGET_LOOP} might actually be faster for multi-target masking since the constant in is smaller.
 * <p>
 * Default value: KEYS_CONTAIN
 */
public enum JsonMultiTargetAlgorithm {
    SINGLE_TARGET_LOOP,
    KEYS_CONTAIN
}
