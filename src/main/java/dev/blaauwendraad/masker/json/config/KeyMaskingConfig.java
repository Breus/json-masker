package dev.blaauwendraad.masker.json.config;

import javax.annotation.CheckForNull;
import java.util.Objects;

public class KeyMaskingConfig {
    private final String maskStringsWith;
    private final String maskStringCharactersWith;
    private final boolean disableNumberMasking;
    private final String maskNumbersWithString;
    private final Integer maskNumbersWith;
    private final Integer maskNumberDigitsWith;
    private final boolean disableBooleanMasking;
    private final String maskBooleansWithString;
    private final Boolean maskBooleansWith;

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
        this.maskStringsWith = maskStringsWith;
        this.maskStringCharactersWith = maskStringCharactersWith;
        this.disableNumberMasking = disableNumberMasking;
        this.maskNumbersWithString = maskNumbersWithString;
        this.maskNumbersWith = maskNumbersWith;
        this.maskNumberDigitsWith = maskNumberDigitsWith;
        this.disableBooleanMasking = disableBooleanMasking;
        this.maskBooleansWithString = maskBooleansWithString;
        this.maskBooleansWith = maskBooleansWith;
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
    public String getMaskStringsWith() {
        return maskStringsWith;
    }

    /**
     * @see Builder#maskStringCharactersWith(String)
     */
    @CheckForNull
    public String getMaskStringCharactersWith() {
        return maskStringCharactersWith;
    }

    /**
     * @see Builder#disableNumberMasking()
     */
    public boolean isDisableNumberMasking() {
        return disableNumberMasking;
    }

    /**
     * @see Builder#maskNumbersWith(String)
     */
    @CheckForNull
    public String getMaskNumbersWithString() {
        return maskNumbersWithString;
    }

    /**
     * @see Builder#maskNumbersWith(int)
     */
    @CheckForNull
    public Integer getMaskNumbersWith() {
        return maskNumbersWith;
    }

    /**
     * @see Builder#maskNumberDigitsWith(int)
     */
    @CheckForNull
    public Integer getMaskNumberDigitsWith() {
        return maskNumberDigitsWith;
    }

    /**
     * @see Builder#disableBooleanMasking()
     */
    public boolean isDisableBooleanMasking() {
        return disableBooleanMasking;
    }

    /**
     * @see Builder#maskBooleansWith(String)
     */
    @CheckForNull
    public String getMaskBooleansWithString() {
        return maskBooleansWithString;
    }

    /**
     * @see Builder#maskBooleansWith(boolean)
     */
    @CheckForNull
    public Boolean getMaskBooleansWith() {
        return maskBooleansWith;
    }

    @Override
    public String toString() {
        return "KeyMaskingConfig{" +
                "maskStringsWith='" + maskStringsWith + '\'' +
                ", maskStringCharactersWith='" + maskStringCharactersWith + '\'' +
                ", disableNumberMasking=" + disableNumberMasking +
                ", maskNumbersWithString='" + maskNumbersWithString + '\'' +
                ", maskNumbersWith=" + maskNumbersWith +
                ", maskNumberDigitsWith=" + maskNumberDigitsWith +
                ", disableBooleanMasking=" + disableBooleanMasking +
                ", maskBooleansWithString='" + maskBooleansWithString + '\'' +
                ", maskBooleansWith=" + maskBooleansWith +
                '}';
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
         * For example, "maskMe": "secret" -> "maskMe": "***".
         * <p>
         * Masking strings with '***' is the default behaviour if no string masking option is set.
         *
         * @see #maskStringCharactersWith(String)
         *
         * @return the builder instance
         */
        public Builder maskStringsWith(String value) {
            if (maskStringsWith != null) {
                throw new IllegalStateException("'maskStringsWith(String)' was already set");
            }
            checkMutuallyExclusiveStringMaskingOptions();
            maskStringsWith = value;
            return this;
        }

        /**
         * Mask all characters of string values with the provided character, preserving the length.
         * For example, "maskMe": "secret" -> "maskMe": "******".
         *
         * @see #maskStringsWith(String)
         *
         * @return the builder instance
         */
        public Builder maskStringCharactersWith(String value) {
            if (maskStringCharactersWith != null) {
                throw new IllegalStateException("'maskStringCharactersWith(String)' was already set");
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
                throw new IllegalStateException("'disableNumberMasking()' was already set");
            }
            checkMutuallyExclusiveNumberMaskingOptions();
            disableNumberMasking = true;
            return this;
        }

        /**
         * Mask all number values with the provided value.
         * For example, "maskMe": 12345 -> "maskMe": "###".
         * <p>
         * Masking numbers with '###' is the default behaviour if no number masking option is set.
         *
         * @see #disableBooleanMasking()
         * @see #maskNumbersWith(int)
         * @see #maskNumberDigitsWith(int)
         *
         * @return the builder instance
         */
        public Builder maskNumbersWith(String value) {
            if (maskNumbersWithString != null) {
                throw new IllegalStateException("'maskNumbersWith(String)' was already set");
            }
            checkMutuallyExclusiveNumberMaskingOptions();
            maskNumbersWithString = Objects.requireNonNull(value);
            return this;
        }

        /**
         * Mask all number values with the provided value.
         * For example, "maskMe": 12345 -> "maskMe": 0.
         *
         * @see #disableBooleanMasking()
         * @see #maskNumbersWith(String)
         * @see #maskNumberDigitsWith(int)
         *
         * @return the builder instance
         */
        public Builder maskNumbersWith(int value) {
            if (maskNumbersWith != null) {
                throw new IllegalStateException("'maskNumbersWith(int)' was already set");
            }
            checkMutuallyExclusiveNumberMaskingOptions();
            maskNumbersWith = value;
            return this;
        }

        /**
         * Mask all digits of number values with the provided digit, preserving the length.
         * For example, "maskMe": 12345 -> "maskMe": 88888.
         *
         * @see #disableBooleanMasking()
         * @see #maskNumbersWith(int)
         * @see #maskNumbersWith(String)
         *
         * @return the builder instance
         */
        public Builder maskNumberDigitsWith(int digit) {
            if (maskNumberDigitsWith != null) {
                throw new IllegalStateException("'maskNumberDigitsWith(int)' was already set");
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
                throw new IllegalStateException("'disableBooleanMasking()' was already set");
            }
            checkMutuallyExclusiveBooleanMaskingOptions();
            disableBooleanMasking = true;
            return this;
        }

        /**
         * Mask all boolean values with the provided value.
         * For example, {@literal "maskMe": true -> "maskMe": "&&&"}.
         * <p>
         * Masking booleans with {@literal '&&&'} is the default behaviour if no boolean masking option is set.
         *
         * @see #disableBooleanMasking()
         * @see #maskBooleansWith(boolean)
         *
         * @return the builder instance
         */
        public Builder maskBooleansWith(String value) {
            if (maskBooleansWith != null) {
                throw new IllegalStateException("'maskBooleansWith(String)' was already set");
            }
            checkMutuallyExclusiveBooleanMaskingOptions();
            maskBooleansWithString = Objects.requireNonNull(value);
            return this;
        }

        /**
         * Mask all boolean values with the provided value.
         * For example, "maskMe": true -> "maskMe": false.
         *
         * @see #disableBooleanMasking()
         * @see #maskBooleansWith(String)
         *
         * @return the builder instance
         */
        public Builder maskBooleansWith(boolean value) {
            if (maskBooleansWith != null) {
                throw new IllegalStateException("'maskBooleansWith(boolean)' was already set");
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
            return new KeyMaskingConfig(
                    maskStringsWith,
                    maskStringCharactersWith,
                    disableNumberMasking != null && disableNumberMasking,
                    maskNumbersWithString,
                    maskNumbersWith,
                    maskNumberDigitsWith,
                    disableBooleanMasking != null && disableBooleanMasking,
                    maskBooleansWithString,
                    maskBooleansWith);
        }

        private void checkMutuallyExclusiveStringMaskingOptions() {
            if (maskStringsWith != null || maskStringCharactersWith != null) {
                throw new IllegalStateException("'maskStringsWith(String)' and 'maskStringCharactersWith(String)' are mutually exclusive");
            }
        }

        private void checkMutuallyExclusiveNumberMaskingOptions() {
            if (disableNumberMasking != null || maskNumbersWith != null || maskNumbersWithString != null || maskNumberDigitsWith != null) {
                throw new IllegalStateException("'disableNumberMasking()', 'maskNumbersWith(int)', 'maskNumbersWith(String)' and 'maskNumberDigitsWith(int)' are mutually exclusive");
            }
        }

        private void checkMutuallyExclusiveBooleanMaskingOptions() {
            if (disableBooleanMasking != null || maskBooleansWith != null || maskBooleansWithString != null) {
                throw new IllegalStateException("'disableBooleanMasking()', 'maskBooleansWith(boolean)' and 'maskBooleansWith(String)' are mutually exclusive");
            }
        }
    }
}