package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class CustomKeyMaskingConfigTest {

    @Test
    void maskingWithDefaultConfig() {
        JsonMasker jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .maskKeys(Set.of("string", "number", "boolean"))
                .maskJsonPaths(Set.of("$.stringPath", "$.numberPath", "$.booleanPath"))
                .build());

        String masked = jsonMasker.mask("""
                {
                  "string": "maskMe",
                  "stringPath": "maskMe",
                  "number": 12345,
                  "numberPath": 12345,
                  "boolean": false,
                  "booleanPath": false
                }
                """);
        Assertions.assertThat(masked).isEqualTo("""
                {
                  "string": "***",
                  "stringPath": "***",
                  "number": "###",
                  "numberPath": "###",
                  "boolean": "&&&",
                  "booleanPath": "&&&"
                }
                """
        );
    }

    @Test
    void maskingWithCustomConfig() {
        JsonMasker jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .maskKeys(Set.of("string", "number", "boolean"))
                .maskJsonPaths(Set.of("$.stringPath", "$.numberPath", "$.booleanPath"))
                .maskStringsWith("(string)")
                .maskNumbersWith("(number)")
                .maskBooleansWith("(boolean)")
                .build());

        String masked = jsonMasker.mask("""
                {
                  "string": "maskMe",
                  "stringPath": "maskMe",
                  "number": 12345,
                  "numberPath": 12345,
                  "boolean": false,
                  "booleanPath": false
                }
                """);
        Assertions.assertThat(masked).isEqualTo("""
                {
                  "string": "(string)",
                  "stringPath": "(string)",
                  "number": "(number)",
                  "numberPath": "(number)",
                  "boolean": "(boolean)",
                  "booleanPath": "(boolean)"
                }
                """
        );
    }

    @Test
    void maskingWithCustomConfigForTheKey() {
        JsonMasker jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .maskKeys(Set.of("string", "number", "boolean"))
                .maskJsonPaths(Set.of("$.stringPath", "$.numberPath", "$.booleanPath"))
                .maskKeys(Set.of("stringCustom", "numberCustom", "booleanCustom"), config ->
                        config.maskStringsWith("(string)")
                                .maskNumbersWith("(number)")
                                .maskBooleansWith("(boolean)")
                )
                .maskJsonPaths(Set.of("$.stringPathCustom", "$.numberPathCustom", "$.booleanPathCustom"), config ->
                        config.maskStringsWith("(string)")
                                .maskNumbersWith("(number)")
                                .maskBooleansWith("(boolean)")
                )
                .build());

        String masked = jsonMasker.mask("""
                {
                  "string": "maskMe",
                  "stringPath": "maskMe",
                  "stringCustom": "maskMe",
                  "stringPathCustom": "maskMe",
                  "number": 12345,
                  "numberPath": 12345,
                  "numberCustom": 12345,
                  "numberPathCustom": 12345,
                  "boolean": false,
                  "booleanPath": false,
                  "booleanCustom": false,
                  "booleanPathCustom": false
                }
                """);
        Assertions.assertThat(masked).isEqualTo("""
                {
                  "string": "***",
                  "stringPath": "***",
                  "stringCustom": "(string)",
                  "stringPathCustom": "(string)",
                  "number": "###",
                  "numberPath": "###",
                  "numberCustom": "(number)",
                  "numberPathCustom": "(number)",
                  "boolean": "&&&",
                  "booleanPath": "&&&",
                  "booleanCustom": "(boolean)",
                  "booleanPathCustom": "(boolean)"
                }
                """
        );
    }

    @Test
    void maskingConfigsForConfigsShouldBeInherited() {
        JsonMasker jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .maskKeys(Set.of("string", "number", "boolean", "a"))
                .maskKeys(Set.of("b"), config ->
                        config.maskStringsWith("(mask_b.string)")
                                .maskNumbersWith("(mask_b.number)")
                                .maskBooleansWith("(mask_b.boolean)")
                )
                .maskKeys(Set.of("c"), config ->
                        config.maskStringsWith("(mask_c.string)")
                                .maskNumbersWith("(mask_c.number)")
                                .maskBooleansWith("(mask_c.boolean)")
                )
                .build());

        String masked = jsonMasker.mask("""
                {
                  "a": {
                    "string": "maskMe",
                    "number": 12345,
                    "boolean": false,
                    "b": {
                      "string": "maskMe",
                      "number": 12345,
                      "boolean": false,
                      "c": {
                        "string": "maskMe",
                        "number": 12345,
                        "boolean": false
                      },
                      "e": {
                        "string": "maskMe",
                        "number": 12345,
                        "boolean": false
                      }
                    },
                    "d": {
                      "string": "maskMe",
                      "number": 12345,
                      "boolean": false
                    }
                  }
                }
                """);

        Assertions.assertThat(masked).isEqualTo("""
                {
                  "a": {
                    "string": "***",
                    "number": "###",
                    "boolean": "&&&",
                    "b": {
                      "string": "(mask_b.string)",
                      "number": "(mask_b.number)",
                      "boolean": "(mask_b.boolean)",
                      "c": {
                        "string": "(mask_c.string)",
                        "number": "(mask_c.number)",
                        "boolean": "(mask_c.boolean)"
                      },
                      "e": {
                        "string": "(mask_b.string)",
                        "number": "(mask_b.number)",
                        "boolean": "(mask_b.boolean)"
                      }
                    },
                    "d": {
                      "string": "***",
                      "number": "###",
                      "boolean": "&&&"
                    }
                  }
                }
                """);
    }

    @Test
    void maskingConfigsForConfigsShouldBeInheritedForJsonPath() {
        JsonMasker jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .maskKeys(Set.of("string", "number", "boolean"))
                .maskJsonPaths("$.a")
                .maskJsonPaths(Set.of("$.a.b"), config ->
                        config.maskStringsWith("(mask_b.string)")
                                .maskNumbersWith("(mask_b.number)")
                                .maskBooleansWith("(mask_b.boolean)")
                )
                .maskJsonPaths(Set.of("$.a.b.c"), config ->
                        config.maskStringsWith("(mask_c.string)")
                                .maskNumbersWith("(mask_c.number)")
                                .maskBooleansWith("(mask_c.boolean)")
                )
                .build());

        String masked = jsonMasker.mask("""
                {
                  "a": {
                    "string": "maskMe",
                    "number": 12345,
                    "boolean": false,
                    "b": {
                      "string": "maskMe",
                      "number": 12345,
                      "boolean": false,
                      "c": {
                        "string": "maskMe",
                        "number": 12345,
                        "boolean": false
                      },
                      "e": {
                        "string": "maskMe",
                        "number": 12345,
                        "boolean": false
                      }
                    },
                    "d": {
                      "string": "maskMe",
                      "number": 12345,
                      "boolean": false
                    }
                  }
                }
                """);

        Assertions.assertThat(masked).isEqualTo("""
                {
                  "a": {
                    "string": "***",
                    "number": "###",
                    "boolean": "&&&",
                    "b": {
                      "string": "(mask_b.string)",
                      "number": "(mask_b.number)",
                      "boolean": "(mask_b.boolean)",
                      "c": {
                        "string": "(mask_c.string)",
                        "number": "(mask_c.number)",
                        "boolean": "(mask_c.boolean)"
                      },
                      "e": {
                        "string": "(mask_b.string)",
                        "number": "(mask_b.number)",
                        "boolean": "(mask_b.boolean)"
                      }
                    },
                    "d": {
                      "string": "***",
                      "number": "###",
                      "boolean": "&&&"
                    }
                  }
                }
                """);
    }
}
