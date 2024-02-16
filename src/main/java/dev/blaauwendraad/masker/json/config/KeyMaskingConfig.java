package dev.blaauwendraad.masker.json.config;

import javax.annotation.CheckForNull;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class KeyMaskingConfig {
    @CheckForNull
    private final byte[] maskStringsWith;
    @CheckForNull
    private final byte[] maskStringCharactersWith;
    private final boolean disableNumberMasking;
    @CheckForNull
    private final byte[] maskNumbersWith;
    @CheckForNull
    private final byte[] maskNumberDigitsWith;
    private final boolean disableBooleanMasking;
    @CheckForNull
    private final byte[] maskBooleansWith;

    KeyMaskingConfig(
            @CheckForNull String maskStringsWith,
            @CheckForNull String maskStringCharactersWith,
            boolean disableNumberMasking,
            @CheckForNull String maskNumbersWithString,
            @CheckForNull Integer maskNumbersWith,
            @CheckForNull Integer maskNumberDigitsWith,
            boolean disableBooleanMasking,
            @CheckForNull String maskBooleansWithString,
            @CheckForNull Boolean maskBooleansWith) {
        if (maskStringsWith != null) {
            this.maskStringsWith = maskStringsWith.getBytes(StandardCharsets.UTF_8);
            this.maskStringCharactersWith = null;
        } else if (maskStringCharactersWith != null) {
            this.maskStringsWith = null;
            this.maskStringCharactersWith = maskStringCharactersWith.getBytes(StandardCharsets.UTF_8);
        } else {
            throw new IllegalStateException("One of string masking options must be set");
        }
        if (maskNumbersWithString != null) {
            this.maskNumbersWith = ("\"" + maskNumbersWithString + "\"").getBytes(StandardCharsets.UTF_8);
            this.maskNumberDigitsWith = null;
        } else if (maskNumbersWith != null) {
            this.maskNumbersWith = maskNumbersWith.toString().getBytes(StandardCharsets.UTF_8);
            this.maskNumberDigitsWith = null;
        } else if (maskNumberDigitsWith != null) {
            this.maskNumbersWith = null;
            this.maskNumberDigitsWith = maskNumberDigitsWith.toString().getBytes(StandardCharsets.UTF_8);
        } else if (disableNumberMasking) {
            this.maskNumbersWith = null;
            this.maskNumberDigitsWith = null;
        } else {
            throw new IllegalStateException("One of number masking options must be set");
        }
        this.disableNumberMasking = disableNumberMasking;
        if (maskBooleansWithString != null) {
            this.maskBooleansWith = ("\"" + maskBooleansWithString + "\"").getBytes(StandardCharsets.UTF_8);
        } else if (maskBooleansWith != null) {
            this.maskBooleansWith = maskBooleansWith.toString().getBytes(StandardCharsets.UTF_8);
        } else if (disableBooleanMasking) {
            this.maskBooleansWith = null;
        } else {
            throw new IllegalStateException("One of boolean masking options must be set");
        }
        this.disableBooleanMasking = disableBooleanMasking;
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
        return disableNumberMasking;
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
        return disableBooleanMasking;
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
                ", disableNumberMasking=" + disableNumberMasking +
                ", maskNumbersWith=" + bytesToString(maskNumbersWith) +
                ", maskNumberDigitsWith=" + bytesToString(maskNumberDigitsWith) +
                ", disableBooleanMasking=" + disableBooleanMasking +
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
            if (maskBooleansWith != null) {
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
            if (maskStringsWith == null && maskStringCharactersWith == null) {
                maskStringsWith = "***";
            }
            if (disableBooleanMasking == null && maskNumbersWithString == null && maskNumbersWith == null && maskNumberDigitsWith == null) {
                maskNumbersWithString = "###";
            }
            if (disableBooleanMasking == null && maskBooleansWithString == null && maskBooleansWith == null) {
                maskBooleansWithString = "&&&";
            }
            boolean disableBooleanMasking = this.disableBooleanMasking != null && this.disableBooleanMasking;
            boolean disableNumberMasking = this.disableNumberMasking != null && this.disableNumberMasking;
            return new KeyMaskingConfig(
                    maskStringsWith,
                    maskStringCharactersWith,
                    disableNumberMasking,
                    maskNumbersWithString,
                    maskNumbersWith,
                    maskNumberDigitsWith,
                    disableBooleanMasking,
                    maskBooleansWithString,
                    maskBooleansWith);
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