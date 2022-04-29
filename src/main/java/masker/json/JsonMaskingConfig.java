package masker.json;

import masker.AbstractMaskingConfig;

public class JsonMaskingConfig extends AbstractMaskingConfig {
    private final JsonMultiTargetAlgorithm multiTargetAlgorithm;
    private int maskNumberValuesWith;

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
            this.multiTargetAlgorithm = JsonMultiTargetAlgorithm.SINGLE_TARGET_LOOP; // default multi-target algorithm
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
            return new JsonMaskingConfig(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

    private JsonMaskingConfig(Builder builder) {
        super(builder);
        multiTargetAlgorithm = builder.multiTargetAlgorithm;
        maskNumberValuesWith = builder.maskNumberValuesWith;
    }

    public JsonMultiTargetAlgorithm getMultiTargetAlgorithm() {
        return multiTargetAlgorithm;
    }

    public int getMaskNumberValuesWith() {
        return maskNumberValuesWith;
    }
}
