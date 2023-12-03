package dev.blaauwendraad.masker.json.config;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contains the JSON masker configurations.
 */
public class JsonMaskingConfig {
    /**
     * Specifies the set of JSON keys for which the string/number values should be targeted (either masked or allowed,
     * depending on the configured {@link JsonMaskingConfig#targetKeyMode}.
     */
    private final Set<String> targetKeys;
    /**
     * The target key mode specifies how to the JSON properties corresponding to the target keys are processed.
     */
    private final TargetKeyMode targetKeyMode;
    /**
     * Specifies the algorithm type that will be used for masking.
     */
    private final JsonMaskerAlgorithmType algorithmType;

    /**
     * @see JsonMaskingConfig.Builder#maskNumberValuesWith
     */
    private final int maskNumericValuesWith;
    /**
     * @see JsonMaskingConfig.Builder#maskObjectValues
     */
    private final boolean maskObjectValues;
    /**
     * @see JsonMaskingConfig.Builder#maskArrayValues
     */
    private final boolean maskArrayValues;
    /**
     * @see JsonMaskingConfig.Builder#obfuscationLength(int)
     */
    private final int obfuscationLength;
    /**
     * @see JsonMaskingConfig.Builder#caseSensitiveTargetKeys
     */
    private final boolean caseSensitiveTargetKeys;

    /**
     * By default, the correct {@link JsonMaskerAlgorithmType} is resolved based on the input of the builder. The logic
     * for this is as follows:
     * <p>
     * If an algorithm type override is set, this will always be the algorithm used.
     * <p>
     *
     * @param builder the builder object
     */
    JsonMaskingConfig(Builder builder) {
        Set<String> targets = builder.targets;
        targetKeyMode = builder.targetKeyMode;
        obfuscationLength = builder.obfuscationLength;
        maskArrayValues = builder.maskArrayValues;
        maskObjectValues = builder.maskObjectValues;
        if (builder.obfuscationLength == 0 && !(builder.maskNumberValuesWith == 0
                || builder.maskNumberValuesWith == -1)) {
            throw new IllegalArgumentException(
                    "If obfuscation length is set to 0, numeric values are replaced with a single 0, so mask number values with must be 0 or number masking must be disabled");
        }
        if (targetKeyMode == TargetKeyMode.MASK && targets.isEmpty()) {
            throw new IllegalArgumentException("Target keys set in mask mode must contain at least a single target key");
        }
        if (builder.maskNumberValuesWith == 0) {
            if (builder.obfuscationLength < 0 || builder.obfuscationLength > 1) {
                throw new IllegalArgumentException(
                        "Mask number values with can only be 0 if obfuscation length is 0 or 1 to preserve valid JSON");
            }
        } else {
            if (builder.maskNumberValuesWith != -1 && (builder.maskNumberValuesWith < 1
                    || builder.maskNumberValuesWith > 9)) {
                throw new IllegalArgumentException(
                        "Mask number values with must be a digit between 1 and 9 when length obfuscation is disabled or obfuscation length is larger than than 0");
            }
        }
        maskNumericValuesWith = builder.maskNumberValuesWith;

        caseSensitiveTargetKeys = builder.caseSensitiveTargetKeys;
        if (!caseSensitiveTargetKeys) {
            targets = targets.stream().map(String::toLowerCase).collect(Collectors.toSet());
        }

        if (builder.algorithmTypeOverride != null) {
            algorithmType = builder.algorithmTypeOverride;
        } else {
            algorithmType = JsonMaskerAlgorithmType.KEYS_CONTAIN;
        }
        switch (algorithmType) {
            case KEYS_CONTAIN -> targetKeys = targets;
            default -> throw new IllegalStateException("Unknown JSON masking algorithm");
        }
    }

    public static JsonMaskingConfig getDefault(Set<String> targets) {
        return custom(targets, TargetKeyMode.MASK).build();
    }

    /**
     * Creates a new {@link JsonMaskingConfig} builder instance.
     *
     * @param targets       target keys of JSONPaths
     * @param targetKeyMode how to interpret the targets set
     * @return the {@link JsonMaskingConfig} builder instance
     */
    public static JsonMaskingConfig.Builder custom(Set<String> targets, TargetKeyMode targetKeyMode) {
        return new JsonMaskingConfig.Builder(targets, targetKeyMode);
    }

    public JsonMaskerAlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    /**
     * Which number to mask numeric JSON values with (e.g. with value 8, the JSON property 1234 will be masked as
     * 8888).
     *
     * @return the number mask
     */
    public int getMaskNumericValuesWith() {
        return maskNumericValuesWith;
    }

    /**
     * Tests if numeric JSON values are masked
     *
     * @return true if number masking is enabled and false otherwise.
     */
    public boolean isNumberMaskingEnabled() {
        return maskNumericValuesWith != -1;
    }

    /**
     * Tests if object values masking is enabled
     */
    public boolean isObjectValuesMaskingEnabled() {
        return maskObjectValues;
    }

    /**
     * Tests if array values masking is enabled
     *
     * @return true if array value masking is enabled and false otherwise.
     */
    public boolean isArrayMaskingEnabled() {
        return maskArrayValues;
    }

    public TargetKeyMode getTargetKeyMode() {
        return targetKeyMode;
    }

    public Set<String> getTargetKeys() {
        return targetKeys;
    }

    /**
     * Get the obfuscation length configuration value.
     *
     * @return the length of the mask to use for all values to obfuscate the original value length, or -1 if length
     * obfuscation is disabled.
     */
    public int getObfuscationLength() {
        return obfuscationLength;
    }

    /**
     * Tests if length obfuscation is enabled.
     *
     * @return true if length obfuscation is enabled and false otherwise
     */
    public boolean isLengthObfuscationEnabled() {
        return obfuscationLength != -1;
    }

    /**
     * Tests if target keys should be considered case-sensitive.
     *
     * @return true if target keys are considered case-sensitive and false otherwise.
     */
    public boolean caseSensitiveTargetKeys() {
        return caseSensitiveTargetKeys;
    }

    /**
     * Builder to create {@link JsonMaskingConfig} instances using the builder pattern.
     */
    public static class Builder {
        private final Set<String> targets;
        private final TargetKeyMode targetKeyMode;
        private int maskNumberValuesWith;

        private boolean maskArrayValues;

        private boolean maskObjectValues;
        private JsonMaskerAlgorithmType algorithmTypeOverride;
        private int obfuscationLength;
        private boolean caseSensitiveTargetKeys;

        public Builder(Set<String> targets, TargetKeyMode targetKeyMode) {
            // targets can be either target keys or target JSON paths
            this.targets = targets;
            this.targetKeyMode = targetKeyMode;
            // by default, mask number values with is -1 which means number value masking is disabled
            this.maskNumberValuesWith = -1;
            // by default, array value masking is enabled
            this.maskArrayValues = true;
            // by default, object value masking is enabled
            this.maskObjectValues = true;
            // by default, length obfuscation is disabled
            this.obfuscationLength = -1;
            // by default, target keys are considered case-insensitive
            this.caseSensitiveTargetKeys = false;
        }

        /**
         * Specifies the number with which numeric values should be replaced. -1 denotes number masking is disabled.
         * <p>
         * Default value: -1 (numeric values are not masked)
         *
         * @param maskNumericValuesWith the number to mask numeric JSON properties with
         * @return the builder instance
         */
        public Builder maskNumericValuesWith(int maskNumericValuesWith) {
            this.maskNumberValuesWith = maskNumericValuesWith;
            return this;
        }

        /**
         * Overrides the automatically chosen masking algorithm {@link JsonMaskerAlgorithmType#KEYS_CONTAIN}.
         *
         * @param algorithmType the override algorithm which will be used
         * @return the builder instance
         */
        public Builder algorithmTypeOverride(JsonMaskerAlgorithmType algorithmType) {
            this.algorithmTypeOverride = algorithmType;
            return this;
        }

        /**
         * @param obfuscationLength specifies the fixed length of the mask when target value lengths is obfuscated. E.g.
         *                          masking any string value with obfuscation length 2 results in "**".
         *                          <p>
         *                          Default value: -1 (length obfuscation disabled).
         * @return the builder instance
         */
        public Builder obfuscationLength(int obfuscationLength) {
            this.obfuscationLength = obfuscationLength;
            return this;
        }

        /**
         * Configures whether the target keys are considered case-sensitive (e.g. cvv != CVV)
         * <p>
         * Default value: false (target keys are considered case-insensitive)
         *
         * @return the builder instance
         */
        public Builder caseSensitiveTargetKeys() {
            this.caseSensitiveTargetKeys = true;
            return this;
        }

        /**
         * Disable array value masking, which is enabled by default
         * <p>
         * Default value: array value masking is enabled by default
         */
        public Builder disableArrayValueMasking() {
            this.maskArrayValues = false;
            return this;
        }

        /**
         * Disable object value masking, which is enabled by default
         * <p>
         * Default value: object value masking is enabled by default
         */
        public Builder disableObjectValueMasking() {
            this.maskObjectValues = false;
            return this;
        }

        /**
         * Creates a new {@link JsonMaskingConfig} instance.
         *
         * @return the new instance
         */
        public JsonMaskingConfig build() {
            return new JsonMaskingConfig(this);
        }
    }

    /**
     * Defines how target keys should be interpreted.
     */
    public enum TargetKeyMode {
        /**
         * In this mode, target keys are interpreted as the only JSON keys for which the corresponding property is
         * allowed (should not be masked).
         */
        ALLOW,
        /**
         * In the mode, target keys are interpreted as the only JSON keys for which the corresponding property should be
         * masked.
         */
        MASK
    }
}
