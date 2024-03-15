package dev.blaauwendraad.masker.json.config;

import dev.blaauwendraad.masker.json.ValueMasker;

import javax.annotation.CheckForNull;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class KeyMaskingConfig {
    private final ValueMasker maskStringsWith;
    private final ValueMasker maskNumbersWith;
    private final ValueMasker maskBooleansWith;

    KeyMaskingConfig(KeyMaskingConfig.Builder builder) {
        this.maskStringsWith = Objects.requireNonNullElseGet(
                builder.maskStringsWith,
                () -> ValueMasker.maskWith("***")
        );
        this.maskNumbersWith = Objects.requireNonNullElseGet(
                builder.maskNumbersWith,
                () -> ValueMasker.maskWith("###")
        );
        this.maskBooleansWith = Objects.requireNonNullElseGet(
                builder.maskBooleansWith,
                () -> ValueMasker.maskWith("&&&")
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
// TODO: it would be nice to have it back for debuggability purposes
//    @Override
//    public String toString() {
//        return "KeyMaskingConfig{" +
//                "maskStringsWith='" + bytesToString(maskStringsWith) + '\'' +
//                ", maskStringCharactersWith='" + bytesToString(maskStringCharactersWith) + '\'' +
//                ", maskNumbersWith=" + bytesToString(maskNumbersWith) +
//                ", maskNumberDigitsWith=" + bytesToString(maskNumberDigitsWith) +
//                ", maskBooleansWith=" + bytesToString(maskBooleansWith) +
//                '}';
//    }

    private String bytesToString(@CheckForNull byte[] bytes) {
        return bytes != null ? new String(bytes, StandardCharsets.UTF_8) : null;
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
         * @see #maskStringCharactersWith(String)
         *
         * @return the builder instance
         */
        public Builder maskStringsWith(String value) {
            checkMutuallyExclusiveStringMaskingOptions();
            maskStringsWith = ValueMasker.maskWith(value);
            return this;
        }

        /**
         * Mask all characters of string values with the provided character, preserving the length.
         * For example, {@literal "maskMe": "secret" -> "maskMe": "******"}.
         *
         * @see #maskStringsWith(String)
         *
         * @return the builder instance
         */
        public Builder maskStringCharactersWith(String value) {
            checkMutuallyExclusiveStringMaskingOptions();
            maskStringsWith = ValueMasker.maskStringCharactersWith(value);
            return this;
        }

        public Builder maskStringsWith(ValueMasker valueMasker) {
            checkMutuallyExclusiveStringMaskingOptions();
            maskStringsWith = valueMasker;
            return this;
        }

        /**
         * Disables number masking.
         *
         * @see #maskNumbersWith(String)
         * @see #maskNumbersWith(int)
         * @see #maskNumberDigitsWith(int)
         *
         * @return the builder instance
         */
        public Builder disableNumberMasking() {
            checkMutuallyExclusiveNumberMaskingOptions();
            maskNumbersWith = ValueMasker.noop();
            return this;
        }

        /**
         * Mask all number values with the provided value.
         * For example, {@literal "maskMe": 12345 -> "maskMe": "###"}.
         * <p>
         * Masking numbers with {@literal "###"} is the default behaviour if no number masking option is set.
         *
         * @see #disableBooleanMasking()
         * @see #maskNumbersWith(int)
         * @see #maskNumberDigitsWith(int)
         *
         * @return the builder instance
         */
        public Builder maskNumbersWith(String value) {
            checkMutuallyExclusiveNumberMaskingOptions();
            maskNumbersWith = ValueMasker.maskWith(value);
            return this;
        }

        /**
         * Mask all number values with the provided value.
         * For example, {@literal "maskMe": 12345 -> "maskMe": 0}.
         *
         * @see #disableBooleanMasking()
         * @see #maskNumbersWith(String)
         * @see #maskNumberDigitsWith(int)
         *
         * @return the builder instance
         */
        public Builder maskNumbersWith(int value) {
            if (maskNumbersWith != null) {
                throw new IllegalArgumentException("'maskNumbersWith(int)' was already set");
            }
            checkMutuallyExclusiveNumberMaskingOptions();
            maskNumbersWith = ValueMasker.maskWith(value);
            return this;
        }

        /**
         * Mask all digits of number values with the provided digit, preserving the length.
         * For example, {@literal "maskMe": 12345 -> "maskMe": 88888}.
         *
         * @see #disableBooleanMasking()
         * @see #maskNumbersWith(int)
         * @see #maskNumbersWith(String)
         *
         * @return the builder instance
         */
        public Builder maskNumberDigitsWith(int digit) {
            checkMutuallyExclusiveNumberMaskingOptions();
            maskNumbersWith = ValueMasker.maskNumberDigitsWith(digit);
            return this;
        }

        public Builder maskNumbersWith(ValueMasker valueMasker) {
            checkMutuallyExclusiveNumberMaskingOptions();
            maskNumbersWith = valueMasker;
            return this;
        }

        /**
         * Disables boolean masking.
         *
         * @see #maskBooleansWith(String)
         * @see #maskBooleansWith(boolean)
         *
         * @return the builder instance
         */
        public Builder disableBooleanMasking() {
            checkMutuallyExclusiveBooleanMaskingOptions();
            maskBooleansWith = ValueMasker.noop();
            return this;
        }

        /**
         * Mask all boolean values with the provided value.
         * For example, {@literal "maskMe": true -> "maskMe": "&&&"}.
         * <p>
         * Masking booleans with {@literal "&&&"} is the default behaviour if no boolean masking option is set.
         *
         * @see #disableBooleanMasking()
         * @see #maskBooleansWith(boolean)
         *
         * @return the builder instance
         */
        public Builder maskBooleansWith(String value) {
            checkMutuallyExclusiveBooleanMaskingOptions();
            maskBooleansWith = ValueMasker.maskWith(value);
            return this;
        }

        /**
         * Mask all boolean values with the provided value.
         * For example, {@literal "maskMe": true -> "maskMe": false}.
         *
         * @see #disableBooleanMasking()
         * @see #maskBooleansWith(String)
         *
         * @return the builder instance
         */
        public Builder maskBooleansWith(boolean value) {
            checkMutuallyExclusiveBooleanMaskingOptions();
            maskBooleansWith = ValueMasker.maskWith(value);
            return this;
        }

        public Builder maskBooleansWith(ValueMasker valueMasker) {
            checkMutuallyExclusiveBooleanMaskingOptions();
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

        private void checkMutuallyExclusiveStringMaskingOptions() {
            if (maskStringsWith != null) {
                throw new IllegalArgumentException("'maskStringsWith(String)' and 'maskStringCharactersWith(String)' are mutually exclusive and cannot be set twice");
            }
        }

        private void checkMutuallyExclusiveNumberMaskingOptions() {
            if (maskNumbersWith != null) {
                throw new IllegalArgumentException("'disableNumberMasking()', 'maskNumbersWith(int)', 'maskNumbersWith(String)' and 'maskNumberDigitsWith(int)' are mutually exclusive");
            }
        }

        private void checkMutuallyExclusiveBooleanMaskingOptions() {
            if (maskBooleansWith != null) {
                throw new IllegalArgumentException("'disableBooleanMasking()', 'maskBooleansWith(boolean)' and 'maskBooleansWith(String)' are mutually exclusive");
            }
        }
    }
}