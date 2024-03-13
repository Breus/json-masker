package dev.blaauwendraad.masker.json.config;

import javax.annotation.CheckForNull;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class KeyMaskingConfig {
    @CheckForNull
    private final byte[] maskStringsWith;
    @CheckForNull
    private final byte[] maskStringCharactersWith;
    @CheckForNull
    private final byte[] maskNumbersWith;
    @CheckForNull
    private final byte[] maskNumberDigitsWith;
    @CheckForNull
    private final byte[] maskBooleansWith;

    KeyMaskingConfig(KeyMaskingConfig.Builder builder) {
        if (builder.maskStringsWith != null) {
            this.maskStringsWith = ("\"" + builder.maskStringsWith + "\"").getBytes(StandardCharsets.UTF_8);
            this.maskStringCharactersWith = null;
        } else if (builder.maskStringCharactersWith != null) {
            this.maskStringsWith = null;
            this.maskStringCharactersWith = builder.maskStringCharactersWith.getBytes(StandardCharsets.UTF_8);
        } else {
            this.maskStringsWith = "\"***\"".getBytes(StandardCharsets.UTF_8);
            this.maskStringCharactersWith = null;
        }
        if (builder.maskNumbersWithString != null) {
            this.maskNumbersWith = ("\"" + builder.maskNumbersWithString + "\"").getBytes(StandardCharsets.UTF_8);
            this.maskNumberDigitsWith = null;
        } else if (builder.maskNumbersWith != null) {
            this.maskNumbersWith = builder.maskNumbersWith.toString().getBytes(StandardCharsets.UTF_8);
            this.maskNumberDigitsWith = null;
        } else if (builder.maskNumberDigitsWith != null) {
            this.maskNumbersWith = null;
            this.maskNumberDigitsWith = builder.maskNumberDigitsWith.toString().getBytes(StandardCharsets.UTF_8);
        } else if (builder.disableNumberMasking != null && builder.disableNumberMasking) {
            this.maskNumbersWith = null;
            this.maskNumberDigitsWith = null;
        } else {
            this.maskNumbersWith = "\"###\"".getBytes(StandardCharsets.UTF_8);
            this.maskNumberDigitsWith = null;
        }
        if (builder.maskBooleansWithString != null) {
            this.maskBooleansWith = ("\"" + builder.maskBooleansWithString + "\"").getBytes(StandardCharsets.UTF_8);
        } else if (builder.maskBooleansWith != null) {
            this.maskBooleansWith = builder.maskBooleansWith.toString().getBytes(StandardCharsets.UTF_8);
        } else if (builder.disableBooleanMasking != null && builder.disableBooleanMasking) {
            this.maskBooleansWith = null;
        } else {
            this.maskBooleansWith = "\"&&&\"".getBytes(StandardCharsets.UTF_8);
        }
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
     * @see Builder#maskStringsWith(String)
     */
    @CheckForNull
    public byte[] getMaskStringsWith() {
        return maskStringsWith;
    }

    /**
     * @see Builder#maskStringCharactersWith(String)
     */
    @CheckForNull
    public byte[] getMaskStringCharactersWith() {
        return maskStringCharactersWith;
    }

    /**
     * @see Builder#disableNumberMasking()
     */
    public boolean isDisableNumberMasking() {
        return maskNumbersWith == null && maskNumberDigitsWith == null;
    }

    /**
     * @see Builder#maskNumbersWith(int)
     */
    @CheckForNull
    public byte[] getMaskNumbersWith() {
        return maskNumbersWith;
    }

    /**
     * @see Builder#maskNumberDigitsWith(int)
     */
    @CheckForNull
    public byte[] getMaskNumberDigitsWith() {
        return maskNumberDigitsWith;
    }

    /**
     * @see Builder#disableBooleanMasking()
     */
    public boolean isDisableBooleanMasking() {
        return maskBooleansWith == null;
    }

    /**
     * @see Builder#maskBooleansWith(boolean)
     */
    @CheckForNull
    public byte[] getMaskBooleansWith() {
        return maskBooleansWith;
    }

    @Override
    public String toString() {
        return "KeyMaskingConfig{" +
                "maskStringsWith='" + bytesToString(maskStringsWith) + '\'' +
                ", maskStringCharactersWith='" + bytesToString(maskStringCharactersWith) + '\'' +
                ", maskNumbersWith=" + bytesToString(maskNumbersWith) +
                ", maskNumberDigitsWith=" + bytesToString(maskNumberDigitsWith) +
                ", maskBooleansWith=" + bytesToString(maskBooleansWith) +
                '}';
    }

    private String bytesToString(@CheckForNull byte[] bytes) {
        return bytes != null ? new String(bytes, StandardCharsets.UTF_8) : null;
    }

    public static class Builder {

        // String masking, mutually exclusive options
        private String maskStringsWith;
        private String maskStringCharactersWith;

        // Number masking, mutually exclusive options
        private Boolean disableNumberMasking;
        private String maskNumbersWithString;
        private Integer maskNumbersWith;
        private Integer maskNumberDigitsWith;

        // Boolean masking, mutually exclusive options
        private Boolean disableBooleanMasking;
        private String maskBooleansWithString;
        private Boolean maskBooleansWith;

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
            if (maskStringsWith != null) {
                throw new IllegalArgumentException("'maskStringsWith(String)' was already set");
            }
            checkMutuallyExclusiveStringMaskingOptions();
            maskStringsWith = value;
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
            if (maskStringCharactersWith != null) {
                throw new IllegalArgumentException("'maskStringCharactersWith(String)' was already set");
            }
            checkMutuallyExclusiveStringMaskingOptions();
            maskStringCharactersWith = value;
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
            if (disableNumberMasking != null) {
                throw new IllegalArgumentException("'disableNumberMasking()' was already set");
            }
            checkMutuallyExclusiveNumberMaskingOptions();
            disableNumberMasking = true;
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
            if (maskNumbersWithString != null) {
                throw new IllegalArgumentException("'maskNumbersWith(String)' was already set");
            }
            checkMutuallyExclusiveNumberMaskingOptions();
            maskNumbersWithString = Objects.requireNonNull(value);
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
            maskNumbersWith = value;
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
            if (maskNumberDigitsWith != null) {
                throw new IllegalArgumentException("'maskNumberDigitsWith(int)' was already set");
            }
            checkMutuallyExclusiveNumberMaskingOptions();
            if (digit < 1 || digit > 9) {
                throw new IllegalArgumentException("Masking digit must be between 1 and 9 to avoid leading zeroes");
            }
            maskNumberDigitsWith = digit;
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
            if (disableBooleanMasking != null) {
                throw new IllegalArgumentException("'disableBooleanMasking()' was already set");
            }
            checkMutuallyExclusiveBooleanMaskingOptions();
            disableBooleanMasking = true;
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
            if (maskBooleansWithString != null) {
                throw new IllegalArgumentException("'maskBooleansWith(String)' was already set");
            }
            checkMutuallyExclusiveBooleanMaskingOptions();
            maskBooleansWithString = Objects.requireNonNull(value);
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
            if (maskBooleansWith != null) {
                throw new IllegalArgumentException("'maskBooleansWith(boolean)' was already set");
            }
            checkMutuallyExclusiveBooleanMaskingOptions();
            maskBooleansWith = value;
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
            if (maskStringsWith != null || maskStringCharactersWith != null) {
                throw new IllegalArgumentException("'maskStringsWith(String)' and 'maskStringCharactersWith(String)' are mutually exclusive");
            }
        }

        private void checkMutuallyExclusiveNumberMaskingOptions() {
            if (disableNumberMasking != null || maskNumbersWith != null || maskNumbersWithString != null || maskNumberDigitsWith != null) {
                throw new IllegalArgumentException("'disableNumberMasking()', 'maskNumbersWith(int)', 'maskNumbersWith(String)' and 'maskNumberDigitsWith(int)' are mutually exclusive");
            }
        }

        private void checkMutuallyExclusiveBooleanMaskingOptions() {
            if (disableBooleanMasking != null || maskBooleansWith != null || maskBooleansWithString != null) {
                throw new IllegalArgumentException("'disableBooleanMasking()', 'maskBooleansWith(boolean)' and 'maskBooleansWith(String)' are mutually exclusive");
            }
        }
    }
}