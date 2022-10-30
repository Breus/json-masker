package masker.json;

import masker.AbstractMaskingConfig;

public class JsonMaskingConfig extends AbstractMaskingConfig {
    /**
     * Specifies the algorithm that will be used to mask multiple
     */
    private final JsonMultiTargetAlgorithm multiTargetAlgorithm;
    /**
     * Specifies the number with which numeric values should be replaced.
     * -1 denotes number musking is disabled.
     * <p>
     * Default value: -1
     */
    private final int maskNumberValuesWith;

    public static JsonMaskingConfig getDefault() {
        return custom().build();
    }

    public static JsonMaskingConfig.Builder custom() {
        return new JsonMaskingConfig.Builder();
    }

    public static class Builder extends AbstractMaskingConfig.Builder<Builder> {
        private JsonMultiTargetAlgorithm multiTargetAlgorithm;
        private int maskNumberValuesWith;

        public Builder() {
            this.multiTargetAlgorithm = JsonMultiTargetAlgorithm.KEYS_CONTAIN; // default multi-target algorithm
            this.maskNumberValuesWith = -1; // default value -1 means number value masking is disabled
        }

        public Builder multiTargetAlgorithm(JsonMultiTargetAlgorithm multiTargetAlgorithm) {
            this.multiTargetAlgorithm = multiTargetAlgorithm;
            return this;
        }

        public Builder maskNumberValuesWith(int maskNumberValuesWith) {
            this.maskNumberValuesWith = maskNumberValuesWith;
            return this;
        }

        @Override
        public JsonMaskingConfig build() {
            if (maskNumberValuesWith == 0) {
                if (getObfuscationLength() < 0 || getObfuscationLength() > 1) {
                    throw new IllegalArgumentException("Mask number values with can only be 0 if obfuscation length is 0 or 1 to preserve valid JSON");
                }
            } else {
                if (maskNumberValuesWith != -1 && (maskNumberValuesWith < 1 || maskNumberValuesWith > 9)) {
                    throw new IllegalArgumentException("Mask number values with must be a digit between 1 and 9 when length obfuscation is disabled or obfuscation length is larger than than 0");
                }
            }
            return new JsonMaskingConfig(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

    private JsonMaskingConfig(Builder builder) {
        super(builder);
        if (builder.getObfuscationLength() == 0 && !(builder.maskNumberValuesWith == 0 || builder.maskNumberValuesWith == -1)) {
            throw new IllegalArgumentException("If obfuscation length is set to 0, numeric values are replaced with a single 0, so mask number values with must be 0 or number masking must be disabled");
        }
        multiTargetAlgorithm = builder.multiTargetAlgorithm;
        maskNumberValuesWith = builder.maskNumberValuesWith;
    }

    public JsonMultiTargetAlgorithm getMultiTargetAlgorithm() {
        return multiTargetAlgorithm;
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
}
