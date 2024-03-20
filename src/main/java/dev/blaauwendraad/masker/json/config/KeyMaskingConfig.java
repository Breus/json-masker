package dev.blaauwendraad.masker.json.config;

import dev.blaauwendraad.masker.json.ValueMasker;
import dev.blaauwendraad.masker.json.ValueMaskers;

import java.util.Objects;

public final class KeyMaskingConfig {
    private final ValueMasker.StringMasker maskStringsWith;
    private final ValueMasker.NumberMasker maskNumbersWith;
    private final ValueMasker.BooleanMasker maskBooleansWith;

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
     * Returns a {@link ValueMasker} to mask a string value.
     */
    public ValueMasker.StringMasker getStringValueMasker() {
        return maskStringsWith;
    }

    /**
     * Returns a {@link ValueMasker} to mask a number value.
     */
    public ValueMasker.NumberMasker getNumberValueMasker() {
        return maskNumbersWith;
    }

    /**
     * Returns a {@link ValueMasker} to mask a number value.
     */
    public ValueMasker.BooleanMasker getBooleanValueMasker() {
        return maskBooleansWith;
    }

    @Override
    public String toString() {
        return "maskStringsWith=%s, maskNumbersWith=%s, maskBooleansWith=%s"
                .formatted(maskStringsWith, maskNumbersWith, maskBooleansWith);
    }

    public static class Builder {
        private ValueMasker.StringMasker maskStringsWith;
        private ValueMasker.NumberMasker maskNumbersWith;
        private ValueMasker.BooleanMasker maskBooleansWith;

        private Builder() {
        }

        /**
         * Mask all string values with the provided value.
         * <p> For example, {@literal "maskMe": "secret" -> "maskMe": "***"}.
         * <p> Masking strings with {@literal "***"} is the default behaviour if no string masking option is set.
         *
         * @return the builder instance
         * @see #maskStringCharactersWith(String)
         * @see #maskStringsWith(ValueMasker.StringMasker)
         */
        public Builder maskStringsWith(String value) {
            maskStringsWith(ValueMaskers.with(value));
            return this;
        }

        /**
         * Mask all characters of string values with the provided character, preserving the length.
         * <p> For example, {@literal "maskMe": "secret" -> "maskMe": "******"}.
         *
         * @return the builder instance
         * @see #maskStringsWith(String)
         * @see #maskStringsWith(ValueMasker.StringMasker)
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
         * @see ValueMaskers for out-of-the-box implementations
         */
        public Builder maskStringsWith(ValueMasker.StringMasker valueMasker) {
            if (maskStringsWith != null) {
                throw new IllegalArgumentException("'maskStringsWith' was already set");
            }
            maskStringsWith = valueMasker;
            return this;
        }

        /**
         * Mask all numeric values with the provided value.
         * <p> For example, {@literal "maskMe": 12345 -> "maskMe": "###"}.
         * <p>
         * Masking numbers with {@literal "###"} is the default behaviour if no number masking option is set.
         *
         * @return the builder instance
         * @see #maskNumbersWith(int)
         * @see #maskNumberDigitsWith(int)
         * @see #maskNumbersWith(ValueMasker.NumberMasker)
         */
        public Builder maskNumbersWith(String value) {
            maskNumbersWith(ValueMaskers.with(value));
            return this;
        }

        /**
         * Mask all numeric values with the provided value.
         * <p> For example, {@literal "maskMe": 12345 -> "maskMe": 0}.
         *
         * @return the builder instance
         * @see #maskNumbersWith(String)
         * @see #maskNumberDigitsWith(int)
         * @see #maskNumbersWith(ValueMasker.NumberMasker)
         */
        public Builder maskNumbersWith(int value) {
            maskNumbersWith(ValueMaskers.with(value));
            return this;
        }

        /**
         * Mask all digits of numeric values with the provided digit, preserving the length.
         * <p> For example, {@literal "maskMe": 12345 -> "maskMe": 88888}.
         *
         * @return the builder instance
         * @see #maskNumbersWith(String)
         * @see #maskNumbersWith(int)
         * @see #maskNumbersWith(ValueMasker.NumberMasker)
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
         * @see ValueMaskers for out-of-the-box implementations
         */
        public Builder maskNumbersWith(ValueMasker.NumberMasker valueMasker) {
            if (maskNumbersWith != null) {
                throw new IllegalArgumentException("'maskNumbersWith' was already set");
            }
            maskNumbersWith = valueMasker;
            return this;
        }

        /**
         * Mask all boolean values with the provided value.
         * <p> For example, {@literal "maskMe": true -> "maskMe": "&&&"}.
         * <p> Masking booleans with {@literal "&&&"} is the default behaviour if no boolean masking option is set.
         *
         * @return the builder instance
         * @see #maskBooleansWith(boolean)
         * @see #maskBooleansWith(ValueMasker.BooleanMasker)
         */
        public Builder maskBooleansWith(String value) {
            maskBooleansWith(ValueMaskers.with(value));
            return this;
        }

        /**
         * Mask all boolean values with the provided value.
         * <p> For example, {@literal "maskMe": true -> "maskMe": false}.
         *
         * @return the builder instance
         * @see #maskBooleansWith(String)
         * @see #maskBooleansWith(ValueMasker.BooleanMasker)
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
         * @see ValueMaskers for out-of-the-box implementations
         */
        public Builder maskBooleansWith(ValueMasker.BooleanMasker valueMasker) {
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