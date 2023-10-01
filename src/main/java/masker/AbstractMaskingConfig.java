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

    /**
     * Specifies whether targets should be considered case-sensitive.
     * <p>
     * Default value: false
     */
    private final boolean caseSensitiveTargets;

    protected AbstractMaskingConfig(Builder<?> builder) {
        obfuscationLength = builder.obfuscationLength;
        caseSensitiveTargets = builder.caseSensitiveTargets;
    }

    public int getObfuscationLength() {
        return obfuscationLength;
    }

    public boolean isObfuscationEnabled() {
        return obfuscationLength != -1;
    }

    public boolean isCaseSensitiveTargets() {
        return caseSensitiveTargets;
    }

    protected abstract static class Builder<T extends Builder<T>> {
        private int obfuscationLength = -1;
        private boolean caseSensitiveTargets = false;

        public T obfuscationLength(int obfuscationLength) {
            this.obfuscationLength = obfuscationLength;
            return self();
        }

        public T caseSensitiveTargets(boolean caseSensitiveTargets) {
            this.caseSensitiveTargets = caseSensitiveTargets;
            return self();
        }

        public int getObfuscationLength() {
            return obfuscationLength;
        }

        public boolean isCaseSensitiveTargets() {
            return caseSensitiveTargets;
        }

        public abstract AbstractMaskingConfig build();

        // Subclasses must override this method to return "this"
        protected abstract T self();
    }
}
