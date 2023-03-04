package masker.json.config;

import masker.AbstractMaskingConfig;
import masker.json.path.JsonPath;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonMaskingConfig extends AbstractMaskingConfig {
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
     * Specifies the number with which numeric values should be replaced.
     * -1 denotes number musking is disabled.
     * <p>
     * Default value: -1
     */
    private final int maskNumberValuesWith;

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
        super(builder);
        if (builder.getObfuscationLength() == 0 && !(builder.maskNumberValuesWith == 0
                || builder.maskNumberValuesWith == -1)) {
            throw new IllegalArgumentException(
                    "If obfuscation length is set to 0, numeric values are replaced with a single 0, so mask number values with must be 0 or number masking must be disabled");
        }
        if (builder.targets == null || builder.targets.isEmpty()) {
            throw new IllegalArgumentException("At least a single target have to be set");
        }
        maskNumberValuesWith = builder.maskNumberValuesWith;

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

    public static JsonMaskingConfig getDefault(@NotNull Set<String> targets) {
        return custom(targets).build();
    }

    public static JsonMaskingConfig.Builder custom(@NotNull Set<String> targets) {
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

    public static class Builder extends AbstractMaskingConfig.Builder<Builder> {
        private final Set<String> targets;
        private int maskNumberValuesWith;

        private boolean resolveJsonPaths;

        private JsonMaskerAlgorithmType algorithmTypeOverride;

        public Builder(@NotNull Set<@NotNull String> targets) {
            this.targets = targets;
            // by default, mask number values with is -1 which means number value masking is disabled
            this.maskNumberValuesWith = -1;
            // by default, JSON paths are resolved, every target starting with "$." is considered a JSONPath
            this.resolveJsonPaths = true;
        }

        public Builder maskNumberValuesWith(int maskNumberValuesWith) {
            this.maskNumberValuesWith = maskNumberValuesWith;
            return this;
        }

        public Builder disableJsonPathResolving() {
            this.resolveJsonPaths = false;
            return this;
        }

        public Builder algorithmTypeOverride(@NotNull JsonMaskerAlgorithmType algorithmType) {
            this.algorithmTypeOverride = algorithmType;
            return this;
        }

        @Override
        public JsonMaskingConfig build() {
            if (targets.isEmpty()) {
                throw new IllegalArgumentException("Target key set must contain at least on target key");
            }
            if (maskNumberValuesWith == 0) {
                if (getObfuscationLength() < 0 || getObfuscationLength() > 1) {
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

        @Override
        protected Builder self() {
            return this;
        }
    }
}
