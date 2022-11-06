package masker;

public abstract class AbstractMaskingConfig {
    /**
     * Specifies the fixed length of the mask when target value lengths is obfuscated.
     * E.g. masking any string value with obfuscation length 2 results in "**".
     * <p>
     * -1 means length obfuscation is disabled.
     * <p>
     * Default value: -1 (via builder).
     */
    private final int obfuscationLength;

    public int getObfuscationLength() {
        return obfuscationLength;
    }

    public boolean isObfuscationEnabled() {
        return obfuscationLength != -1;
    }

    protected abstract static class Builder<T extends Builder<T>> {
        private int obfuscationLength = -1;

        public T obfuscationLength(int obfuscationLength) {
            this.obfuscationLength = obfuscationLength;
            return self();
        }

        public int getObfuscationLength() {
            return obfuscationLength;
        }

        public abstract AbstractMaskingConfig build();

        // Subclasses must override this method to return "this"
        protected abstract T self();
    }

    protected AbstractMaskingConfig(Builder<?> builder) {
        obfuscationLength = builder.obfuscationLength;
    }
}
