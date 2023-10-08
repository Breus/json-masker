package dev.blaauwendraad.masker.json.config;

import dev.blaauwendraad.masker.json.path.JsonPath;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonMaskingConfig {
    /**
     * Specifies the set of JSON keys for which the string/number values should be masked.
     */
    private final Set<String> targetKeys;
    /**
     * Specifies the set of JSON paths for which the string/number values should be masked.
     */
    private final Set<JsonPath> targetJsonPaths;
    /**
     * Specifies the algorithm type that will be used for masking.
     */
    private final JsonMaskerAlgorithmType algorithmType;

    /**
     * @see JsonMaskingConfig.Builder#maskNumberValuesWith
     */
    private final int maskNumberValuesWith;
    /**
     * @see JsonMaskingConfig.Builder#obfuscationLength(int)
     */
    private final int obfuscationLength;
    /**
     * @see JsonMaskingConfig.Builder#caseSensitiveTargetKeys
     */
    private final boolean caseSensitiveTargetKeys;

    /**
     * By default, the correct {@link JsonMaskerAlgorithmType} is resolved based on the input of the builder.
     * The logic for this is as follows:
     * <p>
     * If an algorithm type override is set, this will always be the algorithm used.
     * If this algorithm is JSONPath-aware, the target keys that start with "$." will be interpreted as JSONPaths.
     * If the algorithm is not JSONPath-aware, all targets will be interpreted as regular targets (even if they start with "$."),
     * in which case this prefix will just be interpreted as part of the target key.
     * <p>
     * If no algorithm type override is set, the algorithm is selected as following:
     * If the target set contains Strings starting with "$.", these will be interpreted as JSONPaths, and the
     * JSONPath-aware algorithm is used.
     * If the target set does not contain JSONPaths, the {@link JsonMaskerAlgorithmType#KEYS_CONTAIN} will be chosen if
     * the target set contains more than one target key or {@link JsonMaskerAlgorithmType#SINGLE_TARGET_LOOP}.
     *
     * @param builder the builder object
     */
    JsonMaskingConfig(Builder builder) {
        obfuscationLength = builder.obfuscationLength;
        if (builder.obfuscationLength == 0 && !(builder.maskNumberValuesWith == 0
                || builder.maskNumberValuesWith == -1)) {
            throw new IllegalArgumentException(
                    "If obfuscation length is set to 0, numeric values are replaced with a single 0, so mask number values with must be 0 or number masking must be disabled");
        }
        if (builder.targets == null || builder.targets.isEmpty()) {
            throw new IllegalArgumentException("At least a single target have to be set");
        }
        maskNumberValuesWith = builder.maskNumberValuesWith;

        caseSensitiveTargetKeys = builder.caseSensitiveTargetKeys;
        if (!caseSensitiveTargetKeys) {
            builder.targets = builder.targets.stream().map(String::toLowerCase).collect(Collectors.toSet());
        }

        Set<String> jsonPathLiterals = builder.targets.stream()
                .filter(t -> t.startsWith("$."))
                .collect(Collectors.toSet());
        if (builder.algorithmTypeOverride != null) {
            algorithmType = builder.algorithmTypeOverride;
        } else if (!jsonPathLiterals.isEmpty() && builder.resolveJsonPaths) {
            algorithmType = JsonMaskerAlgorithmType.PATH_AWARE_KEYS_CONTAIN;
        } else if (builder.targets.size() > 1) {
            algorithmType = JsonMaskerAlgorithmType.KEYS_CONTAIN;
        } else {
            algorithmType = JsonMaskerAlgorithmType.SINGLE_TARGET_LOOP;
        }

        switch (algorithmType) {
            case PATH_AWARE_KEYS_CONTAIN -> {
                targetJsonPaths = resolveJsonPaths(jsonPathLiterals);
                HashSet<String> targets = new HashSet<>(builder.targets);
                targets.removeAll(jsonPathLiterals);
                targetKeys = targets;
            }
            case KEYS_CONTAIN, SINGLE_TARGET_LOOP -> {
                targetJsonPaths = Set.of();
                targetKeys = builder.targets;
            }
            default -> throw new IllegalStateException("Unknown JSON masking algorithm");
        }
    }

    private Set<JsonPath> resolveJsonPaths(Set<String> targets) {
        return targets.stream().map(JsonPath::from).collect(Collectors.toSet());
    }

    public static JsonMaskingConfig getDefault(Set<String> targets) {
        return custom(targets).build();
    }

    public static JsonMaskingConfig.Builder custom(Set<String> targets) {
        return new JsonMaskingConfig.Builder(targets);
    }

    public JsonMaskerAlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public int getMaskNumberValuesWith() {
        return maskNumberValuesWith;
    }

    public boolean isNumberMaskingEnabled() {
        return maskNumberValuesWith != -1;
    }

    public boolean isNumberMaskingDisabled() {
        return maskNumberValuesWith == -1;
    }

    public Set<String> getTargetKeys() {
        return targetKeys;
    }

    public Set<JsonPath> getTargetJsonPaths() {
        return targetJsonPaths;
    }

    public int getObfuscationLength() {
        return obfuscationLength;
    }

    public boolean isObfuscationEnabled() {
        return obfuscationLength != -1;
    }

    public boolean caseSensitiveTargetKeys() {
        return caseSensitiveTargetKeys;
    }

    public static class Builder {
        private Set<String> targets;
        private int maskNumberValuesWith;
        private boolean resolveJsonPaths;
        private JsonMaskerAlgorithmType algorithmTypeOverride;
        private int obfuscationLength;
        private boolean caseSensitiveTargetKeys;

        public Builder(Set<String> targets) {
            this.targets = targets;
            // by default, mask number values with is -1 which means number value masking is disabled
            this.maskNumberValuesWith = -1;
            // by default, JSON paths are resolved, every target starting with "$." is interpreted as a JSONPath
            this.resolveJsonPaths = true;
            // by default, length obfuscation is disabled
            this.obfuscationLength = -1;
            // by default, target keys are considered case-insensitive
            this.caseSensitiveTargetKeys = false;
        }

        /**
         * Specifies the number with which numeric values should be replaced.
         * -1 denotes number masking is disabled.
         * <p>
         * Default value: -1
         */
        public Builder maskNumberValuesWith(int maskNumberValuesWith) {
            this.maskNumberValuesWith = maskNumberValuesWith;
            return this;
        }

        /**
         * Overrides the automatically chosen masking algorithm {@link JsonMaskerAlgorithmType#KEYS_CONTAIN}.
         * @param algorithmType the override algorithm which will be used
         */
        public Builder algorithmTypeOverride(JsonMaskerAlgorithmType algorithmType) {
            this.algorithmTypeOverride = algorithmType;
            return this;
        }

        /**
         * @param obfuscationLength specifies the fixed length of the mask when target value lengths is obfuscated.
         * E.g. masking any string value with obfuscation length 2 results in "**".
         * <p>
         * -1 means length obfuscation is disabled.
         * <p>
         * Default value: -1 (disabled).
         */
        public Builder obfuscationLength(int obfuscationLength) {
            this.obfuscationLength = obfuscationLength;
            return self();
        }

        /**
         * Configures whether the target keys are considered case-sensitive (e.g. cvv != CVV)
         * <p>
         * Default value: false (target keys are considered case-insensitive)
         */
        public Builder caseSensitiveTargetKeys() {
            this.caseSensitiveTargetKeys = true;
            return self();
        }

        /**
         * Disables that target keys starting with a '$' are interpreted as JSON paths
         * <p>
         * Default value: true (JSON path resolving is enabled)
         */
        public Builder disableJsonPathResolving() {
            this.resolveJsonPaths = false;
            return this;
        }

        public JsonMaskingConfig build() {
            if (targets.isEmpty()) {
                throw new IllegalArgumentException("Target key set must contain at least on target key");
            }
            if (maskNumberValuesWith == 0) {
                if (obfuscationLength < 0 || obfuscationLength > 1) {
                    throw new IllegalArgumentException(
                            "Mask number values with can only be 0 if obfuscation length is 0 or 1 to preserve valid JSON");
                }
            } else {
                if (maskNumberValuesWith != -1 && (maskNumberValuesWith < 1 || maskNumberValuesWith > 9)) {
                    throw new IllegalArgumentException(
                            "Mask number values with must be a digit between 1 and 9 when length obfuscation is disabled or obfuscation length is larger than than 0");
                }
            }
            return new JsonMaskingConfig(this);
        }

        protected Builder self() {
            return this;
        }
    }
}
