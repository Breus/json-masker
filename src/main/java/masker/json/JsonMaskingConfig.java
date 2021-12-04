package masker.json;

import masker.AbstractMaskingConfig;

public class JsonMaskingConfig extends AbstractMaskingConfig {
    private final JsonMultiTargetAlgorithm multiTargetAlgorithm;

    public static JsonMaskingConfig getDefault() {
        return custom().build();
    }

    public static JsonMaskingConfig.Builder custom() {
        return new JsonMaskingConfig.Builder();
    }

    public static class Builder extends AbstractMaskingConfig.Builder<Builder> {
        private JsonMultiTargetAlgorithm multiTargetAlgorithm;

        public Builder() {
            this.multiTargetAlgorithm = JsonMultiTargetAlgorithm.SINGLE_TARGET_LOOP; // default multi-target algorithm
        }

        public Builder multiTargetAlgorithm(JsonMultiTargetAlgorithm multiTargetAlgorithm) {
            this.multiTargetAlgorithm = multiTargetAlgorithm;
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
    }

    public JsonMultiTargetAlgorithm getMultiTargetAlgorithm() {
        return multiTargetAlgorithm;
    }
}
