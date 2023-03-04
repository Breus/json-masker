package masker.json.config;

import masker.AbstractMaskingConfig;
import masker.json.path.JsonPath;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

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

    private JsonMaskingConfig(Builder builder) {
        super(builder);
        if (builder.getObfuscationLength() == 0 && !(builder.maskNumberValuesWith == 0
                || builder.maskNumberValuesWith == -1)) {
            throw new IllegalArgumentException(
                    "If obfuscation length is set to 0, numeric values are replaced with a single 0, so mask number values with must be 0 or number masking must be disabled");
        }
        maskNumberValuesWith = builder.maskNumberValuesWith;
        Set<JsonPath> tmpTargetJsonPaths = builder.resolveJsonPaths ? resolveJsonPaths(builder.targets) : null;
        if (builder.algorithmTypeOverride == JsonMaskerAlgorithmType.PATH_AWARE_KEYS_CONTAIN ||
                (builder.algorithmTypeOverride == null && tmpTargetJsonPaths != null
                        && !tmpTargetJsonPaths.isEmpty())) {
            algorithmType = JsonMaskerAlgorithmType.PATH_AWARE_KEYS_CONTAIN;
            targetJsonPaths = tmpTargetJsonPaths;
            targetKeys = null;
        } else {
            targetKeys = builder.targets;
            targetJsonPaths = null;
            if (builder.algorithmTypeOverride == JsonMaskerAlgorithmType.KEYS_CONTAIN ||
                    (builder.algorithmTypeOverride == null && builder.targets.size() > 1)) {
                algorithmType = JsonMaskerAlgorithmType.KEYS_CONTAIN;
            } else {
                algorithmType = JsonMaskerAlgorithmType.SINGLE_TARGET_LOOP;
            }
        }
    }

    private Set<JsonPath> resolveJsonPaths(Set<String> targets) {
        //TODO implement JSON path resolving
        return Set.of();
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
        if (algorithmType != JsonMaskerAlgorithmType.KEYS_CONTAIN
                && algorithmType != JsonMaskerAlgorithmType.SINGLE_TARGET_LOOP) {
            throw new IllegalArgumentException("Determined algorithm does not support target keys");
        }
        return targetKeys;
    }

    public Set<JsonPath> getTargetsJsonPaths() {
        if (algorithmType != JsonMaskerAlgorithmType.PATH_AWARE_KEYS_CONTAIN) {
            throw new IllegalArgumentException("Determined algorithm does not support target JSON paths");
        }
        return targetJsonPaths;
    }

    public static class Builder extends AbstractMaskingConfig.Builder<Builder> {
        private final Set<String> targets;
        private int maskNumberValuesWith;

        private boolean resolveJsonPaths;

        private JsonMaskerAlgorithmType algorithmTypeOverride;

        public Builder(@NotNull Set<@NotNull String> targets) {
            this.targets = targets;
            this.maskNumberValuesWith = -1; // default value -1 means number value masking is disabled
            this.resolveJsonPaths = true; // default JSON paths are resolved
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
