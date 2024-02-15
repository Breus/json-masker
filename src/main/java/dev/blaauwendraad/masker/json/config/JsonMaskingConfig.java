package dev.blaauwendraad.masker.json.config;

import dev.blaauwendraad.masker.json.path.JsonPath;
import dev.blaauwendraad.masker.json.path.JsonPathParser;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

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

    JsonMaskingConfig(
            TargetKeyMode targetKeyMode,
            Set<String> targetKeys,
            Set<JsonPath> targetJsonPaths,
            boolean caseSensitiveTargetKeys,
            KeyMaskingConfig defaultConfig,
            Map<String, KeyMaskingConfig> targetKeyConfigs
    ) {
        this.targetKeyMode = targetKeyMode;
        this.targetKeys = targetKeys;
        this.targetJsonPaths = targetJsonPaths;
        this.caseSensitiveTargetKeys = caseSensitiveTargetKeys;
        this.defaultConfig = defaultConfig;
        this.targetKeyConfigs = targetKeyConfigs;
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
     *
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
        return "JsonMaskingConfig{" +
                "targetKeys=" + targetKeys +
                ", targetKeyMode=" + targetKeyMode +
                ", caseSensitiveTargetKeys=" + caseSensitiveTargetKeys +
                ", defaultConfig=" + defaultConfig +
                ", targetKeyConfigs=" + targetKeyConfigs +
                '}';
    }

    /**
     * Builder to create {@link JsonMaskingConfig} instances using the builder pattern.
     */
    public static class Builder {
        private static final JsonPathParser JSON_PATH_PARSER = new JsonPathParser();

        private static final Consumer<KeyMaskingConfig.Builder> NOOP = builder -> {
        };

        private final Set<String> targetKeys = new HashSet<>();
        private final Set<JsonPath> targetJsonPaths = new HashSet<>();
        private TargetKeyMode targetKeyMode;
        private Boolean caseSensitiveTargetKeys;

        private final KeyMaskingConfig.Builder defaultConfigBuilder = KeyMaskingConfig.builder();
        private final Map<String, KeyMaskingConfig> targetKeyConfigs = new HashMap<>();

        public Builder maskKeys(String... keys) {
            return maskKeys(Arrays.asList(keys));
        }

        public Builder maskKeys(Collection<String> keys) {
            return maskKeys(keys, NOOP);
        }

        public Builder maskKeys(String key, Consumer<KeyMaskingConfig.Builder> configurer) {
            return maskKeys(Set.of(key), configurer);
        }

        public Builder maskKeys(Collection<String> keys, Consumer<KeyMaskingConfig.Builder> configurer) {
            if (configurer == NOOP && targetKeyMode == TargetKeyMode.ALLOW) {
                throw new IllegalArgumentException("Cannot mask keys when in ALLOW mode, if you want" +
                        " to customize masking for specific keys in ALLOW mode, use" +
                        " maskKeys(String key, Consumer<KeyMaskingConfig.Builder> configurer)");
            }
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
                if (configurer != NOOP) {
                    KeyMaskingConfig.Builder builder = KeyMaskingConfig.builder();
                    configurer.accept(builder);
                    targetKeyConfigs.put(key, builder.build());
                }
            }
            return this;
        }

        public Builder maskJsonPaths(String... jsonPaths) {
            return maskJsonPaths(Arrays.asList(jsonPaths));
        }

        public Builder maskJsonPaths(Collection<String> jsonPaths) {
            return maskJsonPaths(jsonPaths, NOOP);
        }

        public Builder maskJsonPaths(String jsonPath, Consumer<KeyMaskingConfig.Builder> configurer) {
            return maskJsonPaths(Set.of(jsonPath), configurer);
        }

        public Builder maskJsonPaths(Collection<String> jsonPaths, Consumer<KeyMaskingConfig.Builder> configurer) {
            if (configurer == NOOP && targetKeyMode == TargetKeyMode.ALLOW) {
                throw new IllegalArgumentException("Cannot mask json paths when in ALLOW mode, if you want" +
                        " to customize masking for specific json paths in ALLOW mode, use" +
                        " maskJsonPaths(String key, Consumer<KeyMaskingConfig.Builder> configurer)");
            }
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
                if (configurer != NOOP) {
                    KeyMaskingConfig.Builder builder = KeyMaskingConfig.builder();
                    configurer.accept(builder);
                    targetKeyConfigs.put(parsed.toString(), builder.build());
                }
            }
            return this;
        }

        public Builder allowKeys(String... keys) {
            return allowKeys(Arrays.asList(keys));
        }

        public Builder allowKeys(Collection<String> keys) {
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

        public Builder allowJsonPaths(String... jsonPaths) {
            return allowJsonPaths(Arrays.asList(jsonPaths));
        }

        public Builder allowJsonPaths(Collection<String> jsonPaths) {
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
         * @see #maskStringCharactersWith(String)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskStringsWith(String)
         *
         * @return the builder instance
         */
        public Builder maskStringsWith(String value) {
            defaultConfigBuilder.maskStringsWith(value);
            return this;
        }

        /**
         * Mask all characters of string values with the provided character, preserving the length.
         * For example, "maskMe": "secret" -> "maskMe": "******".
         *
         * @see #maskStringsWith(String)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskStringCharactersWith(String)
         *
         * @return the builder instance
         */
        public Builder maskStringCharactersWith(String value) {
            defaultConfigBuilder.maskStringCharactersWith(value);
            return this;
        }

        /**
         * Disables number masking.
         *
         * @see #maskNumbersWith(String)
         * @see #maskNumbersWith(int)
         * @see #maskNumberDigitsWith(int)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#disableNumberMasking()
         *
         * @return the builder instance
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
         * @see #disableBooleanMasking()
         * @see #maskNumbersWith(int)
         * @see #maskNumberDigitsWith(int)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskNumbersWith(String)
         *
         * @return the builder instance
         */
        public Builder maskNumbersWith(String value) {
            defaultConfigBuilder.maskNumbersWith(value);
            return this;
        }

        /**
         * Mask all number values with the provided value.
         * For example, "maskMe": 12345 -> "maskMe": 0.
         *
         * @see #disableBooleanMasking()
         * @see #maskNumbersWith(String)
         * @see #maskNumberDigitsWith(int)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskNumbersWith(int)
         *
         * @return the builder instance
         */
        public Builder maskNumbersWith(int value) {
            defaultConfigBuilder.maskNumbersWith(value);
            return this;
        }

        /**
         * Mask all digits of number values with the provided digit, preserving the length.
         * For example, "maskMe": 12345 -> "maskMe": 88888.
         *
         * @see #disableBooleanMasking()
         * @see #maskNumbersWith(int)
         * @see #maskNumbersWith(String)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskNumberDigitsWith(int)
         *
         * @return the builder instance
         */
        public Builder maskNumberDigitsWith(int digit) {
            defaultConfigBuilder.maskNumberDigitsWith(digit);
            return this;
        }

        /**
         * Disables boolean masking.
         *
         * @see #maskBooleansWith(String)
         * @see #maskBooleansWith(boolean)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskBooleansWith(String)
         *
         * @return the builder instance
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
         * @see #disableBooleanMasking()
         * @see #maskBooleansWith(boolean)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskBooleansWith(String)
         *
         * @return the builder instance
         */
        public Builder maskBooleansWith(String value) {
            defaultConfigBuilder.maskBooleansWith(value);
            return this;
        }

        /**
         * Mask all boolean values with the provided value.
         * For example, "maskMe": true -> "maskMe": false.
         *
         * @see #disableBooleanMasking()
         * @see #maskBooleansWith(String)
         * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskBooleansWith(boolean)
         *
         * @return the builder instance
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
            if (targetKeyMode == null) {
                throw new IllegalArgumentException("Not keys were requested to mask or allow");
            }
            return new JsonMaskingConfig(
                    targetKeyMode,
                    targetKeys,
                    targetJsonPaths,
                    caseSensitiveTargetKeys != null && caseSensitiveTargetKeys,
                    defaultConfigBuilder.build(),
                    targetKeyConfigs
            );
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
