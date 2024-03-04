package dev.blaauwendraad.masker.json.config;

import dev.blaauwendraad.masker.json.path.JsonPath;
import dev.blaauwendraad.masker.json.path.JsonPathParser;

import javax.annotation.CheckForNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Contains the JSON masker configurations.
 */
public final class JsonMaskingConfig {
    /**
     * The target key mode specifies how to the JSON properties corresponding to the target keys are processed.
     */
    private final TargetKeyMode targetKeyMode;
    /**
     * Specifies the set of JSON keys for which the string/number values should be targeted (either masked or allowed,
     * depending on the configured {@link JsonMaskingConfig#targetKeyMode}.
     */
    private final Set<String> targetKeys;
    /**
     * Specifies the set of JSON paths for which the string/number values should be masked.
     */
    private final Set<JsonPath> targetJsonPaths;
    /**
     * @see JsonMaskingConfig.Builder#caseSensitiveTargetKeys
     */
    private final boolean caseSensitiveTargetKeys;

    private final KeyMaskingConfig defaultConfig;
    private final Map<String, KeyMaskingConfig> targetKeyConfigs;

    JsonMaskingConfig(JsonMaskingConfig.Builder builder) {
        if (builder.targetKeyMode == null) {
            throw new IllegalArgumentException("No keys were requested to mask or allow");
        }
        this.targetKeyMode = builder.targetKeyMode;
        this.targetKeys = builder.targetKeys;
        this.targetJsonPaths = builder.targetJsonPaths;
        this.caseSensitiveTargetKeys = builder.caseSensitiveTargetKeys != null && builder.caseSensitiveTargetKeys;
        this.defaultConfig = builder.defaultConfigBuilder.build();
        this.targetKeyConfigs = builder.targetKeyConfigs;
    }

    /**
     * Creates a new {@link JsonMaskingConfig} builder instance for {@link JsonMaskingConfig}.
     *
     * @return the {@link JsonMaskingConfig} builder instance
     */
    public static JsonMaskingConfig.Builder builder() {
        return new JsonMaskingConfig.Builder();
    }

    /**
     * Checks if the target key mode is set to "ALLOW". If the mode is set to "ALLOW", it means that the target keys are
     * interpreted as the only JSON keys for which the corresponding property is allowed (should not be masked).
     *
     * @return true if the target key mode is set to "ALLOW", false otherwise
     */
    public boolean isInAllowMode() {
        return targetKeyMode == TargetKeyMode.ALLOW;
    }

    /**
     * Checks if the target key mode is set to "MASK". If the mode is set to "MASK", it means that the properties
     * corresponding to the target keys should be masked.
     *
     * @return true if the current target key mode is in "MASK" mode, false otherwise
     */
    public boolean isInMaskMode() {
        return targetKeyMode == TargetKeyMode.MASK;
    }

    public Set<String> getTargetKeys() {
        return targetKeys;
    }

    public Set<JsonPath> getTargetJsonPaths() {
        return targetJsonPaths;
    }

    /**
     * Tests if target keys should be considered case-sensitive.
     *
     * @return true if target keys are considered case-sensitive and false otherwise.
     */
    public boolean caseSensitiveTargetKeys() {
        return caseSensitiveTargetKeys;
    }

    /**
     * Returns the config for the given key. If no specific config is available for the given key, the default config.
     *
     * @param key key to be masked
     * @return the config for the given key
     */
    public KeyMaskingConfig getConfig(String key) {
        return targetKeyConfigs.getOrDefault(key, defaultConfig);
    }

    public KeyMaskingConfig getDefaultConfig() {
        return defaultConfig;
    }

    /**
     * Returns a map with all masking configs per key.
     *
     * @return masking configs per key
     */
    public Map<String, KeyMaskingConfig> getKeyConfigs() {
        return Collections.unmodifiableMap(targetKeyConfigs);
    }

    @Override
    public String toString() {
        return """
                JsonMaskingConfig{
                targetKeys=%s,
                targetJsonPaths=%s,
                targetKeyMode=%s,
                caseSensitiveTargetKeys=%s,
                defaultConfig=%s,
                targetKeyConfigs=%s
                }"""
                .formatted(targetKeys, targetJsonPaths, targetKeyMode, caseSensitiveTargetKeys, defaultConfig, targetKeyConfigs);
    }

    /**
     * Builder to create {@link JsonMaskingConfig} instances using the builder pattern.
     */
    public static class Builder {
        private static final JsonPathParser JSON_PATH_PARSER = new JsonPathParser();

        private final Set<String> targetKeys = new HashSet<>();
        private final Set<JsonPath> targetJsonPaths = new HashSet<>();
        private TargetKeyMode targetKeyMode;
        private Boolean caseSensitiveTargetKeys;

        private final KeyMaskingConfig.Builder defaultConfigBuilder = KeyMaskingConfig.builder();
        private final Map<String, KeyMaskingConfig> targetKeyConfigs = new HashMap<>();

        private Builder() {
        }

        public Builder maskKeys(Set<String> keys) {
            if (targetKeyMode == TargetKeyMode.ALLOW) {
                throw new IllegalArgumentException("Cannot mask keys when in ALLOW mode, if you want" +
                                                   " to customize masking for specific keys in ALLOW mode, use" +
                                                   " maskKeys(String key, KeyMaskingConfig config)");
            }
            return maskKeys0(keys, null);
        }

        public Builder maskKeys(Set<String> keys, KeyMaskingConfig config) {
            return maskKeys0(keys, Objects.requireNonNull(config));
        }

        private Builder maskKeys0(Set<String> keys, @CheckForNull KeyMaskingConfig config) {
            if (keys.isEmpty()) {
                throw new IllegalArgumentException("At least one key must be provided");
            }
            for (String key : keys) {
                if (targetKeys.contains(key) || targetKeyConfigs.containsKey(key)) {
                    throw new IllegalArgumentException("Duplicate key '%s'".formatted(key));
                }
                // in ALLOW mode this method can be used to set a specific masking config for a key
                if (targetKeyMode != TargetKeyMode.ALLOW) {
                    targetKeyMode = TargetKeyMode.MASK;
                    targetKeys.add(key);
                }
                if (config != null) {
                    targetKeyConfigs.put(key, config);
                }
            }
            return this;
        }

        public Builder maskJsonPaths(Set<String> jsonPaths) {
            if (targetKeyMode == TargetKeyMode.ALLOW) {
                throw new IllegalArgumentException("Cannot mask json paths when in ALLOW mode, if you want to customize" +
                                                   " masking for specific json paths in ALLOW mode, use " +
                                                   "maskJsonPaths(String jsonPath, KeyMaskingConfig config)");
            }
            return maskJsonPaths0(jsonPaths, null);
        }

        public Builder maskJsonPaths(Set<String> jsonPaths, KeyMaskingConfig config) {
            return maskJsonPaths0(jsonPaths, Objects.requireNonNull(config));
        }

        private Builder maskJsonPaths0(Set<String> jsonPaths, @CheckForNull KeyMaskingConfig config) {
            if (jsonPaths.isEmpty()) {
                throw new IllegalArgumentException("At least one json path must be provided");
            }
            for (String jsonPath : jsonPaths) {
                JsonPath parsed = JSON_PATH_PARSER.parse(jsonPath);
                if (targetJsonPaths.contains(parsed) || targetKeyConfigs.containsKey(parsed.toString())) {
                    throw new IllegalArgumentException("Duplicate json path '%s'".formatted(jsonPath));
                }
                // in ALLOW mode this method can be used to set a specific masking config for a json path
                if (targetKeyMode != TargetKeyMode.ALLOW) {
                    targetKeyMode = TargetKeyMode.MASK;
                    targetJsonPaths.add(parsed);
                }
                if (config != null) {
                    targetKeyConfigs.put(parsed.toString(), config);
                }
            }
            JSON_PATH_PARSER.checkAmbiguity(targetJsonPaths);
            return this;
        }

        public Builder allowKeys(Set<String> keys) {
            if (targetKeyMode == TargetKeyMode.MASK) {
                throw new IllegalArgumentException("Cannot allow keys when in MASK mode");
            }
            targetKeyMode = TargetKeyMode.ALLOW;
            for (String key : keys) {
                if (targetKeys.contains(key)) {
                    throw new IllegalArgumentException("Duplicate key '%s'".formatted(key));
                }
                targetKeys.add(key);
            }
            return this;
        }

        public Builder allowJsonPaths(Set<String> jsonPaths) {
            if (targetKeyMode == TargetKeyMode.MASK) {
                throw new IllegalArgumentException("Cannot allow keys when in MASK mode");
            }
            // as opposed to allowKeys, that can accept an empty list (i.e. mask everything)
            // the same does not make sense for allowJsonPaths, as this would be equivalent to allowKeys
            if (jsonPaths.isEmpty()) {
                throw new IllegalArgumentException("At least one json path must be provided");
            }
            targetKeyMode = TargetKeyMode.ALLOW;
            for (String jsonPath : jsonPaths) {
                JsonPath parsed = JSON_PATH_PARSER.parse(jsonPath);
                if (targetJsonPaths.contains(parsed)) {
                    throw new IllegalArgumentException("Duplicate json path '%s'".formatted(jsonPath));
                }
                targetJsonPaths.add(parsed);
            }
            JSON_PATH_PARSER.checkAmbiguity(targetJsonPaths);
            return this;
        }

        /**
         * Configures whether the target keys are considered case-sensitive (e.g. cvv != CVV)
         * <p>
         * Default value: false (target keys are considered case-insensitive)
         *
         * @return the builder instance
         */
        public Builder caseSensitiveTargetKeys() {
            if (caseSensitiveTargetKeys != null) {
                throw new IllegalArgumentException("Case sensitivity already set");
            }
            this.caseSensitiveTargetKeys = true;
            return this;
        }

        /**
         * Mask all string values with the provided value.
         * For example, "maskMe": "secret" -> "maskMe": "***".
         * <p>
         * Masking strings with '***' is the default behaviour if no string masking option is set.
         *
         * @return the builder instance
         * @see #maskStringCharactersWith(String)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskStringsWith(String)
         */
        public Builder maskStringsWith(String value) {
            defaultConfigBuilder.maskStringsWith(value);
            return this;
        }

        /**
         * Mask all characters of string values with the provided character, preserving the length.
         * For example, "maskMe": "secret" -> "maskMe": "******".
         *
         * @return the builder instance
         * @see #maskStringsWith(String)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskStringCharactersWith(String)
         */
        public Builder maskStringCharactersWith(String value) {
            defaultConfigBuilder.maskStringCharactersWith(value);
            return this;
        }

        /**
         * Disables number masking.
         *
         * @return the builder instance
         * @see #maskNumbersWith(String)
         * @see #maskNumbersWith(int)
         * @see #maskNumberDigitsWith(int)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#disableNumberMasking()
         */
        public Builder disableNumberMasking() {
            defaultConfigBuilder.disableNumberMasking();
            return this;
        }

        /**
         * Mask all number values with the provided value.
         * For example, "maskMe": 12345 -> "maskMe": "###".
         * <p>
         * Masking numbers with '###' is the default behaviour if no number masking option is set.
         *
         * @return the builder instance
         * @see #disableBooleanMasking()
         * @see #maskNumbersWith(int)
         * @see #maskNumberDigitsWith(int)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskNumbersWith(String)
         */
        public Builder maskNumbersWith(String value) {
            defaultConfigBuilder.maskNumbersWith(value);
            return this;
        }

        /**
         * Mask all number values with the provided value.
         * For example, "maskMe": 12345 -> "maskMe": 0.
         *
         * @return the builder instance
         * @see #disableBooleanMasking()
         * @see #maskNumbersWith(String)
         * @see #maskNumberDigitsWith(int)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskNumbersWith(int)
         */
        public Builder maskNumbersWith(int value) {
            defaultConfigBuilder.maskNumbersWith(value);
            return this;
        }

        /**
         * Mask all digits of number values with the provided digit, preserving the length.
         * For example, "maskMe": 12345 -> "maskMe": 88888.
         *
         * @return the builder instance
         * @see #disableBooleanMasking()
         * @see #maskNumbersWith(int)
         * @see #maskNumbersWith(String)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskNumberDigitsWith(int)
         */
        public Builder maskNumberDigitsWith(int digit) {
            defaultConfigBuilder.maskNumberDigitsWith(digit);
            return this;
        }

        /**
         * Disables boolean masking.
         *
         * @return the builder instance
         * @see #maskBooleansWith(String)
         * @see #maskBooleansWith(boolean)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskBooleansWith(String)
         */
        public Builder disableBooleanMasking() {
            defaultConfigBuilder.disableBooleanMasking();
            return this;
        }

        /**
         * Mask all boolean values with the provided value.
         * For example, {@literal "maskMe": true -> "maskMe": "&&&".}
         * <p>
         * Masking booleans with {@literal '&&&'} is the default behaviour if no boolean masking option is set.
         *
         * @return the builder instance
         * @see #disableBooleanMasking()
         * @see #maskBooleansWith(boolean)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskBooleansWith(String)
         */
        public Builder maskBooleansWith(String value) {
            defaultConfigBuilder.maskBooleansWith(value);
            return this;
        }

        /**
         * Mask all boolean values with the provided value.
         * For example, "maskMe": true -> "maskMe": false.
         *
         * @return the builder instance
         * @see #disableBooleanMasking()
         * @see #maskBooleansWith(String)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskBooleansWith(boolean)
         */
        public Builder maskBooleansWith(boolean value) {
            defaultConfigBuilder.maskBooleansWith(value);
            return this;
        }

        /**
         * Creates a new {@link JsonMaskingConfig} instance.
         *
         * @return the new instance
         */
        public JsonMaskingConfig build() {
            return new JsonMaskingConfig(this);
        }
    }

    /**
     * Defines how target keys should be interpreted.
     */
    public enum TargetKeyMode {
        /**
         * In this mode, target keys are interpreted as the only JSON keys for which the corresponding property is
         * allowed (should not be masked).
         */
        ALLOW,
        /**
         * In the mode, target keys are interpreted as the only JSON keys for which the corresponding property should be
         * masked.
         */
        MASK
    }
}
