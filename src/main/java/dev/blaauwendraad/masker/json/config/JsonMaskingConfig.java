package dev.blaauwendraad.masker.json.config;

import dev.blaauwendraad.masker.json.ValueMasker;
import dev.blaauwendraad.masker.json.path.JsonPath;
import dev.blaauwendraad.masker.json.path.JsonPathParser;
import org.jspecify.annotations.Nullable;

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
     * Specifies the set of JSONPaths for which the string/number values should be masked.
     */
    private final Set<JsonPath> targetJsonPaths;
    /**
     * @see JsonMaskingConfig.Builder#caseSensitiveTargetKeys
     */
    private final boolean caseSensitiveTargetKeys;
    /**
     * Not configurable. Specifies the initial size of the byte array buffer in streaming mode
     * Package private for unit tests
     */
    int bufferSize = 8192;

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
     * @return {@code true} if the target key mode is set to "ALLOW", {@code false} otherwise
     */
    public boolean isInAllowMode() {
        return targetKeyMode == TargetKeyMode.ALLOW;
    }

    /**
     * Checks if the target key mode is set to "MASK". If the mode is set to "MASK", it means that the properties
     * corresponding to the target keys should be masked.
     *
     * @return {@code true} if the current target key mode is in "MASK" mode, {@code false} otherwise
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
     * @return {@code true} if target keys are considered case-sensitive, {@code false} otherwise.
     */
    public boolean caseSensitiveTargetKeys() {
        return caseSensitiveTargetKeys;
    }

    public int bufferSize() {
        return bufferSize;
    }

    /**
     * Returns the config for the given key. If no specific config is available for the given key, returns the default config.
     *
     * @param key key to be masked
     * @return the config for the given key or the default config
     */
    public KeyMaskingConfig getConfig(String key) {
        return targetKeyConfigs.getOrDefault(key, defaultConfig);
    }

    /**
     * Returns the config for the given key. If no specific config is available for the given key, returns {@code null}.
     *
     * @param key key to be masked
     * @return the config for the given key
     */
    public @Nullable KeyMaskingConfig getKeyConfig(String key) {
        return targetKeyConfigs.get(key);
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
               targetKeys=%s,
               targetJsonPaths=%s,
               targetKeyMode=%s,
               caseSensitiveTargetKeys=%s,
               defaultConfig=%s,
               targetKeyConfigs=%s
               """
                .formatted(targetKeys, targetJsonPaths, targetKeyMode, caseSensitiveTargetKeys, defaultConfig, targetKeyConfigs);
    }

    /**
     * Builder to create {@link JsonMaskingConfig} instances using the builder pattern.
     */
    public static class Builder {
        private static final JsonPathParser JSON_PATH_PARSER = new JsonPathParser();

        private final Set<String> targetKeys = new HashSet<>();
        private final Set<JsonPath> targetJsonPaths = new HashSet<>();
        @Nullable
        private TargetKeyMode targetKeyMode;
        @Nullable
        private Boolean caseSensitiveTargetKeys;

        private final KeyMaskingConfig.Builder defaultConfigBuilder = KeyMaskingConfig.builder();
        private final Map<String, KeyMaskingConfig> targetKeyConfigs = new HashMap<>();

        private Builder() {
        }

        /**
         * Masks all JSON values corresponding to the given keys with the default masking configuration.
         *
         * @return the builder instance
         */
        public Builder maskKeys(String... keys) {
            return maskKeys(Set.of(keys));
        }

        /**
         * Masks all JSON values corresponding to the given keys with the default masking configuration.
         *
         * @return the builder instance
         */
        public Builder maskKeys(Set<String> keys) {
            if (keys.isEmpty()) {
                throw new IllegalArgumentException("At least one key must be provided");
            }
            keys.forEach(key -> addMaskKey(key, null));
            return this;
        }

        /**
         * Masks all JSON values corresponding to the given key with the provided masking configuration.
         *
         * @return the builder instance
         */
        public Builder maskKeys(String key, KeyMaskingConfig config) {
            addMaskKey(key, Objects.requireNonNull(config));
            return this;
        }

        /**
         * Masks all JSON values corresponding to the given keys with the provided masking configuration.
         *
         * @return the builder instance
         */
        public Builder maskKeys(Set<String> keys, KeyMaskingConfig config) {
            if (keys.isEmpty()) {
                throw new IllegalArgumentException("At least one key must be provided");
            }
            keys.forEach(key -> addMaskKey(key, Objects.requireNonNull(config)));
            return this;
        }

        /**
         * Masks all JSON values corresponding to the given keys with the provided masking configuration.
         *
         * @return the builder instance
         */
        public Builder maskKeys(Map<String, KeyMaskingConfig> configs) {
            if (configs.isEmpty()) {
                throw new IllegalArgumentException("At least one key must be provided");
            }
            configs.forEach(this::addMaskKey);
            return this;
        }

        private void addMaskKey(String key, @Nullable KeyMaskingConfig config) {
            if (config == null && targetKeyMode == TargetKeyMode.ALLOW) {
                throw new IllegalArgumentException("Cannot mask keys when in ALLOW mode, if you want" +
                                                   " to customize masking for specific keys in ALLOW mode, use" +
                                                   " maskKeys that accepts KeyMaskingConfig");
            }
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

        /**
         * Masks all JSON values corresponding to the given JSONPaths with the default masking configuration.
         *
         * @return the builder instance
         */
        public Builder maskJsonPaths(String... jsonPaths) {
            return maskJsonPaths(Set.of(jsonPaths));
        }

        /**
         * Masks all JSON values corresponding to the given JSONPaths with the default masking configuration.
         *
         * @return the builder instance
         */
        public Builder maskJsonPaths(Set<String> jsonPaths) {
            if (jsonPaths.isEmpty()) {
                throw new IllegalArgumentException("At least one JSONPath must be provided");
            }
            jsonPaths.forEach(jsonPath -> addMaskJsonPath(jsonPath, null));
            return this;
        }

        /**
         * Masks all JSON values corresponding to the given JSONPath with the given masking configuration.
         *
         * @return the builder instance
         */
        public Builder maskJsonPaths(String jsonPath, KeyMaskingConfig config) {
            addMaskJsonPath(jsonPath, Objects.requireNonNull(config));
            return this;
        }

        /**
         * Masks all JSON values corresponding to the given JSONPaths with the given masking configuration.
         *
         * @return the builder instance
         */
        public Builder maskJsonPaths(Set<String> jsonPaths, KeyMaskingConfig config) {
            if (jsonPaths.isEmpty()) {
                throw new IllegalArgumentException("At least one JSONPath must be provided");
            }
            Objects.requireNonNull(config);
            jsonPaths.forEach(jsonPath -> addMaskJsonPath(jsonPath, config));
            return this;
        }

        /**
         * Masks all JSON values corresponding to the given JSONPaths with the given masking configuration.
         *
         * @return the builder instance
         */
        public Builder maskJsonPaths(Map<String, KeyMaskingConfig> configs) {
            if (configs.isEmpty()) {
                throw new IllegalArgumentException("At least one JSONPath must be provided");
            }
            configs.forEach(this::addMaskJsonPath);
            return this;
        }

        private void addMaskJsonPath(String jsonPath, @Nullable KeyMaskingConfig config) {
            if (config == null && targetKeyMode == TargetKeyMode.ALLOW) {
                throw new IllegalArgumentException("Cannot mask JSONPaths when in ALLOW mode, if you want to customize" +
                                                   " masking for specific JSONPaths in ALLOW mode, use" +
                                                   " maskJsonPaths that accepts KeyMaskingConfig");
            }
            JsonPath parsed = JSON_PATH_PARSER.parse(jsonPath);
            if (targetJsonPaths.contains(parsed) || targetKeyConfigs.containsKey(parsed.toString())) {
                throw new IllegalArgumentException("Duplicate JSONPath '%s'".formatted(jsonPath));
            }
            // in ALLOW mode this method can be used to set a specific masking config for a JSONPath
            if (targetKeyMode != TargetKeyMode.ALLOW) {
                targetKeyMode = TargetKeyMode.MASK;
                targetJsonPaths.add(parsed);
            }
            if (config != null) {
                targetKeyConfigs.put(parsed.toString(), config);
            }
        }

        /**
         * Only allow the given key to be unmasked, mask JSON values corresponding to any other key.
         *
         * <p>This method is incompatible with any mask method, except cases when a specific key(s) or JSONPath(s)
         * needs to be masked with a specific {@link KeyMaskingConfig} masking configuration.
         *
         * @return the builder instance
         */
        public Builder allowKeys(String... keys) {
            return allowKeys(Set.of(keys));
        }

        /**
         * Only allow the given keys to be unmasked, mask JSON values corresponding to any other key.
         *
         * <p>This method is incompatible with any mask method, except cases when a specific key(s) or JSONPath(s)
         * needs to be masked with a specific {@link KeyMaskingConfig} masking configuration.
         *
         * @return the builder instance
         */
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

        /**
         * Only allow the given JSONPath to be unmasked, mask JSON values corresponding to any other key.
         *
         * <p>This method is incompatible with any mask method, except cases when a specific key(s) or JSONPath(s)
         * needs to be masked with a specific {@link KeyMaskingConfig} masking configuration.
         *
         * @return the builder instance
         */
        public Builder allowJsonPaths(String... jsonPath) {
            return allowJsonPaths(Set.of(jsonPath));
        }

        /**
         * Only allow the given JSONPaths to be unmasked, mask JSON values corresponding to any other key.
         *
         * <p>This method is incompatible with any mask method, except cases when a specific key(s) or JSONPath(s)
         * needs to be masked with a specific {@link KeyMaskingConfig} masking configuration.
         *
         * @return the builder instance
         */
        public Builder allowJsonPaths(Set<String> jsonPaths) {
            if (targetKeyMode == TargetKeyMode.MASK) {
                throw new IllegalArgumentException("Cannot allow keys when in MASK mode");
            }
            if (jsonPaths.contains("$")) {
                throw new IllegalArgumentException("Root node JSONPath is not allowed in ALLOW mode");
            }
            targetKeyMode = TargetKeyMode.ALLOW;
            for (String jsonPath : jsonPaths) {
                JsonPath parsed = JSON_PATH_PARSER.parse(jsonPath);
                if (targetJsonPaths.contains(parsed)) {
                    throw new IllegalArgumentException("Duplicate JSONPath '%s'".formatted(jsonPath));
                }
                targetJsonPaths.add(parsed);
            }
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
         * @see #maskStringsWith(ValueMasker.StringMasker)
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
         * @see #maskStringsWith(ValueMasker.StringMasker)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskStringCharactersWith(String)
         */
        public Builder maskStringCharactersWith(String value) {
            defaultConfigBuilder.maskStringCharactersWith(value);
            return this;
        }

        /**
         * Mask all string values with the provided {@link ValueMasker}.
         *
         * @return the builder instance
         * @see #maskStringsWith(String)
         * @see #maskStringCharactersWith(String)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskStringsWith(ValueMasker.StringMasker)
         */
        public Builder maskStringsWith(ValueMasker.StringMasker valueMasker) {
            defaultConfigBuilder.maskStringsWith(valueMasker);
            return this;
        }

        /**
         * Mask all number values with the provided value.
         * For example, "maskMe": 12345 -> "maskMe": "###".
         * <p>
         * Masking numbers with '###' is the default behaviour if no number masking option is set.
         *
         * @return the builder instance
         * @see #maskNumbersWith(int)
         * @see #maskNumberDigitsWith(int)
         * @see #maskNumbersWith(ValueMasker.NumberMasker)
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
         * @see #maskNumbersWith(String)
         * @see #maskNumberDigitsWith(int)
         * @see #maskNumbersWith(ValueMasker.NumberMasker)
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
         * @see #maskNumbersWith(String)
         * @see #maskNumbersWith(int)
         * @see #maskNumbersWith(ValueMasker.NumberMasker)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskNumberDigitsWith(int)
         */
        public Builder maskNumberDigitsWith(int digit) {
            defaultConfigBuilder.maskNumberDigitsWith(digit);
            return this;
        }

        /**
         * Mask all numeric values with the provided {@link ValueMasker}.
         *
         * @return the builder instance
         * @see #maskNumbersWith(String)
         * @see #maskNumbersWith(int)
         * @see #maskNumberDigitsWith(int)
         * @see #maskNumbersWith(ValueMasker.NumberMasker)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskNumbersWith(ValueMasker.NumberMasker)
         */
        public Builder maskNumbersWith(ValueMasker.NumberMasker valueMasker) {
            defaultConfigBuilder.maskNumbersWith(valueMasker);
            return this;
        }

        /**
         * Mask all boolean values with the provided value.
         * For example, {@literal "maskMe": true -> "maskMe": "&&&".}
         * <p>
         * Masking booleans with {@literal '&&&'} is the default behaviour if no boolean masking option is set.
         *
         * @return the builder instance
         * @see #maskBooleansWith(boolean)
         * @see #maskBooleansWith(ValueMasker.BooleanMasker)
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
         * @see #maskBooleansWith(String)
         * @see #maskBooleansWith(ValueMasker.BooleanMasker)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskBooleansWith(boolean)
         */
        public Builder maskBooleansWith(boolean value) {
            defaultConfigBuilder.maskBooleansWith(value);
            return this;
        }

        /**
         * Mask all boolean values with the provided {@link ValueMasker}.
         *
         * @return the builder instance
         * @see #maskBooleansWith(boolean)
         * @see #maskBooleansWith(String)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskBooleansWith(ValueMasker.BooleanMasker)
         */
        public Builder maskBooleansWith(ValueMasker.BooleanMasker valueMasker) {
            defaultConfigBuilder.maskBooleansWith(valueMasker);
            return this;
        }

        /**
         * Creates a new {@link JsonMaskingConfig} instance.
         *
         * @return the new instance
         */
        public JsonMaskingConfig build() {
            JSON_PATH_PARSER.checkAmbiguity(targetJsonPaths);
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
