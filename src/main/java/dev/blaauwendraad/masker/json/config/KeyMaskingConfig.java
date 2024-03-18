package dev.blaauwendraad.masker.json.config;

import dev.blaauwendraad.masker.json.ValueMasker;
import dev.blaauwendraad.masker.json.ValueMaskers;

import java.util.Objects;

public final class KeyMaskingConfig {
    private final ValueMasker maskStringsWith;
    private final ValueMasker maskNumbersWith;
    private final ValueMasker maskBooleansWith;

    KeyMaskingConfig(KeyMaskingConfig.Builder builder) {
        this.maskStringsWith = Objects.requireNonNullElseGet(
                builder.maskStringsWith,
                () -> ValueMaskers.with("***")
        );
        this.maskNumbersWith = Objects.requireNonNullElseGet(
                builder.maskNumbersWith,
                () -> ValueMaskers.with("###")
        );
        this.maskBooleansWith = Objects.requireNonNullElseGet(
                builder.maskBooleansWith,
                () -> ValueMaskers.with("&&&")
        );
    }

    /**
     * Creates a new {@link Builder} instance for {@link KeyMaskingConfig}.
     *
     * @return the {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a function to mask a string value.
     */
    public ValueMasker getStringValueMasker() {
        return maskStringsWith;
    }

    /**
     * Returns a function to mask a number value.
     */
    public ValueMasker getNumberValueMasker() {
        return maskNumbersWith;
    }

    /**
     * Returns a function to mask a number value.
     */
    public ValueMasker getBooleanValueMasker() {
        return maskBooleansWith;
    }

    @Override
    public String toString() {
        return "maskStringsWith=%s, maskNumbersWith=%s, maskBooleansWith=%s"
                .formatted(maskStringsWith, maskNumbersWith, maskBooleansWith);
    }

    public static class Builder {
        private ValueMasker maskStringsWith;
        private ValueMasker maskNumbersWith;
        private ValueMasker maskBooleansWith;

        private Builder() {
        }

        /**
         * Mask all string values with the provided value.
         * For example, {@literal "maskMe": "secret" -> "maskMe": "***"}.
         * <p>
         * Masking strings with {@literal "***"} is the default behaviour if no string masking option is set.
         *
         * @return the builder instance
         * @see #maskStringCharactersWith(String)
         * @see #maskStringsWith(ValueMasker)
         */
        public Builder maskStringsWith(String value) {
            maskStringsWith(ValueMaskers.with(value));
            return this;
        }

        /**
         * Mask all characters of string values with the provided character, preserving the length.
         * For example, {@literal "maskMe": "secret" -> "maskMe": "******"}.
         *
         * @return the builder instance
         * @see #maskStringsWith(String)
         * @see #maskStringsWith(ValueMasker)
         */
        public Builder maskStringCharactersWith(String value) {
            maskStringsWith(ValueMaskers.eachCharacterWith(value));
            return this;
        }

        /**
         * Mask all string values with the provided {@link ValueMasker}.
         *
         * @return the builder instance
         * @see #maskStringsWith(String)
         * @see #maskStringCharactersWith(String)
         */
        public Builder maskStringsWith(ValueMasker valueMasker) {
            if (maskStringsWith != null) {
                throw new IllegalArgumentException("'maskStringsWith' was already set");
            }
            maskStringsWith = valueMasker;
            return this;
        }

        /**
         * Mask all numeric values with the provided value.
         * For example, {@literal "maskMe": 12345 -> "maskMe": "###"}.
         * <p>
         * Masking numbers with {@literal "###"} is the default behaviour if no number masking option is set.
         *
         * @return the builder instance
         * @see #maskNumbersWith(int)
         * @see #maskNumberDigitsWith(int)
         * @see #maskNumbersWith(ValueMasker)
         */
        public Builder maskNumbersWith(String value) {
            maskNumbersWith(ValueMaskers.with(value));
            return this;
        }

        /**
         * Mask all numeric values with the provided value.
         * For example, {@literal "maskMe": 12345 -> "maskMe": 0}.
         *
         * @return the builder instance
         * @see #maskNumbersWith(String)
         * @see #maskNumberDigitsWith(int)
         * @see #maskNumbersWith(ValueMasker)
         */
        public Builder maskNumbersWith(int value) {
            maskNumbersWith(ValueMaskers.with(value));
            return this;
        }

        /**
         * Mask all digits of numeric values with the provided digit, preserving the length.
         * For example, {@literal "maskMe": 12345 -> "maskMe": 88888}.
         *
         * @return the builder instance
         * @see #maskNumbersWith(String)
         * @see #maskNumbersWith(int)
         * @see #maskNumbersWith(ValueMasker)
         */
        public Builder maskNumberDigitsWith(int digit) {
            maskNumbersWith(ValueMaskers.eachDigitWith(digit));
            return this;
        }

        /**
         * Mask all numeric values with the provided {@link ValueMasker}.
         *
         * @return the builder instance
         * @see #maskNumbersWith(String)
         * @see #maskNumbersWith(int)
         * @see #maskNumberDigitsWith(int)
         */
        public Builder maskNumbersWith(ValueMasker valueMasker) {
            if (maskNumbersWith != null) {
                throw new IllegalArgumentException("'maskNumbersWith' was already set");
            }
            maskNumbersWith = valueMasker;
            return this;
        }

        /**
         * Mask all boolean values with the provided value.
         * For example, {@literal "maskMe": true -> "maskMe": "&&&"}.
         * <p>
         * Masking booleans with {@literal "&&&"} is the default behaviour if no boolean masking option is set.
         *
         * @return the builder instance
         * @see #maskBooleansWith(boolean)
         * @see #maskBooleansWith(ValueMasker)
         */
        public Builder maskBooleansWith(String value) {
            maskBooleansWith(ValueMaskers.with(value));
            return this;
        }

        /**
         * Mask all boolean values with the provided value.
         * For example, {@literal "maskMe": true -> "maskMe": false}.
         *
         * @return the builder instance
         * @see #maskBooleansWith(String)
         * @see #maskBooleansWith(ValueMasker)
         */
        public Builder maskBooleansWith(boolean value) {
            maskBooleansWith(ValueMaskers.with(value));
            return this;
        }

        /**
         * Mask all boolean values with the provided {@link ValueMasker}.
         *
         * @return the builder instance
         * @see #maskBooleansWith(boolean)
         * @see #maskBooleansWith(String)
         */
        public Builder maskBooleansWith(ValueMasker valueMasker) {
            if (maskBooleansWith != null) {
                throw new IllegalArgumentException("'maskBooleansWith' was already set");
            }
            maskBooleansWith = valueMasker;
            return this;
        }

        /**
         * Builds the {@link KeyMaskingConfig} instance.
         *
         * @return the {@link KeyMaskingConfig} instance
         */
        public KeyMaskingConfig build() {
            return new KeyMaskingConfig(this);
        }
    }
}