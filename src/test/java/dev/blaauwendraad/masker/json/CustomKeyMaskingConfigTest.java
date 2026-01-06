package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class CustomKeyMaskingConfigTest {

    @Test
    void maskingWithDefaultConfig() {
        JsonMasker jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .maskKeys(Set.of("string", "number", "boolean"))
                .maskJsonPaths(Set.of("$.stringPath", "$.numberPath", "$.booleanPath"))
                .build());

        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker, """
                {
                  "string": "maskMe",
                  "stringPath": "maskMe",
                  "number": 12345,
                  "numberPath": 12345,
                  "boolean": false,
                  "booleanPath": false
                }
                """, """
                {
                  "string": "***",
                  "stringPath": "***",
                  "number": "###",
                  "numberPath": "###",
                  "boolean": "&&&",
                  "booleanPath": "&&&"
                }
                """);
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

        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker, """
                {
                  "string": "maskMe",
                  "stringPath": "maskMe",
                  "number": 12345,
                  "numberPath": 12345,
                  "boolean": false,
                  "booleanPath": false
                }
                """, """
                {
                  "string": "(string)",
                  "stringPath": "(string)",
                  "number": "(number)",
                  "numberPath": "(number)",
                  "boolean": "(boolean)",
                  "booleanPath": "(boolean)"
                }
                """);
    }

    @Test
    void maskingWithCustomConfigForTheKey() {
        JsonMasker jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .maskKeys(Set.of("string", "number", "boolean"))
                .maskJsonPaths(Set.of("$.stringPath", "$.numberPath", "$.booleanPath"))
                .maskKeys(Set.of("stringCustom", "numberCustom", "booleanCustom"), KeyMaskingConfig.builder()
                        .maskStringsWith("(string)")
                        .maskNumbersWith("(number)")
                        .maskBooleansWith("(boolean)")
                        .build()
                )
                .maskJsonPaths(Set.of("$.stringPathCustom", "$.numberPathCustom", "$.booleanPathCustom"), KeyMaskingConfig.builder()
                        .maskStringsWith("(string)")
                        .maskNumbersWith("(number)")
                        .maskBooleansWith("(boolean)")
                        .build()
                )
                .build());

        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker, """
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
                """, """
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
                """);
    }

    @Test
    void maskingConfigsForConfigsShouldBeInherited() {
        JsonMasker jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .maskKeys(Set.of("string", "number", "boolean", "a"))
                .maskKeys(Map.of(
                        "b", KeyMaskingConfig.builder()
                                .maskStringsWith("(mask_b.string)")
                                .maskNumbersWith("(mask_b.number)")
                                .maskBooleansWith("(mask_b.boolean)")
                                .build(),
                        "c", KeyMaskingConfig.builder()
                                .maskStringsWith("(mask_c.string)")
                                .maskNumbersWith("(mask_c.number)")
                                .maskBooleansWith("(mask_c.boolean)")
                                .build()
                ))
                .build());

        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker, """
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
                      "d": {
                        "string": "maskMe",
                        "number": 12345,
                        "boolean": false
                      }
                    },
                    "e": {
                      "string": "maskMe",
                      "number": 12345,
                      "boolean": false
                    }
                  }
                }
                """, """
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
                      "d": {
                        "string": "(mask_b.string)",
                        "number": "(mask_b.number)",
                        "boolean": "(mask_b.boolean)"
                      }
                    },
                    "e": {
                      "string": "***",
                      "number": "###",
                      "boolean": "&&&"
                    }
                  }
                }
                """);
    }

    @Test
    void maskingConfigsForConfigsShouldBeInheritedForJsonPaths() {
        JsonMasker jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .maskJsonPaths("$.a")
                .maskJsonPaths(Map.of(
                        "$.a.b", KeyMaskingConfig.builder()
                                .maskStringsWith("(mask_b.string)")
                                .maskNumbersWith("(mask_b.number)")
                                .maskBooleansWith("(mask_b.boolean)")
                                .build(),
                        "$.a.b.c", KeyMaskingConfig.builder()
                                .maskStringsWith("(mask_c.string)")
                                .maskNumbersWith("(mask_c.number)")
                                .maskBooleansWith("(mask_c.boolean)")
                                .build()
                ))
                .build());

        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker, """
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
                      "d": {
                        "string": "maskMe",
                        "number": 12345,
                        "boolean": false
                      }
                    },
                    "e": {
                      "string": "maskMe",
                      "number": 12345,
                      "boolean": false
                    }
                  }
                }
                """, """
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
                      "d": {
                        "string": "(mask_b.string)",
                        "number": "(mask_b.number)",
                        "boolean": "(mask_b.boolean)"
                      }
                    },
                    "e": {
                      "string": "***",
                      "number": "###",
                      "boolean": "&&&"
                    }
                  }
                }
                """);
    }

    @Test
    void maskEmail() {
        JsonMasker jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .maskKeys(Set.of("string", "number", "boolean"))
                .maskKeys("emailPrefixSuffixDomain", KeyMaskingConfig.builder()
                        .maskStringsWith(ValueMaskers.email(2, 2, true, "***"))
                        .build()
                )
                .maskKeys("emailPrefixOnly", KeyMaskingConfig.builder()
                        .maskStringsWith(ValueMaskers.email(2, 0, false, "***"))
                        .build()
                )
                .maskKeys("emailSuffixOnly", KeyMaskingConfig.builder()
                        .maskStringsWith(ValueMaskers.email(0, 2, false, "***"))
                        .build()
                )
                .maskKeys("emailDomainOnly", KeyMaskingConfig.builder()
                        .maskStringsWith(ValueMaskers.email(0, 0, true, "***"))
                        .build()
                )
                .build());

        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker, """
                {
                  "string": "maskMe",
                  "number": 12345,
                  "boolean": false,
                  "emailPrefixSuffixDomain": "agavlyukovskiy@gmail.com",
                  "emailPrefixOnly": "agavlyukovskiy@gmail.com",
                  "emailSuffixOnly": "agavlyukovskiy@gmail.com",
                  "emailDomainOnly": "agavlyukovskiy@gmail.com"
                }
                """, """
                {
                  "string": "***",
                  "number": "###",
                  "boolean": "&&&",
                  "emailPrefixSuffixDomain": "ag***iy@gmail.com",
                  "emailPrefixOnly": "ag***",
                  "emailSuffixOnly": "***om",
                  "emailDomainOnly": "***@gmail.com"
                }
                """);
    }

    @Test
    @SuppressWarnings("NullAway") // https://github.com/uber/NullAway/issues/1011
    void maskWithStringFunction() {
        JsonMasker jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .maskKeys(Set.of("string", "number", "boolean"))
                .maskStringsWith(ValueMaskers.withRawValueFunction(value -> "\"***\""))
                .maskNumbersWith(ValueMaskers.withRawValueFunction(value -> "\"###\""))
                .maskBooleansWith(ValueMaskers.withRawValueFunction(value -> "\"&&&\""))
                .maskKeys("function", KeyMaskingConfig.builder()
                        .maskStringsWith(ValueMaskers.withRawValueFunction(value -> value.replaceAll("\\[this secret]", "***")))
                        .build()
                )
                .maskKeys("functionNull", KeyMaskingConfig.builder()
                        .maskStringsWith(ValueMaskers.withRawValueFunction(value -> null))
                        .build()
                )
                .maskKeys("functionConditional", KeyMaskingConfig.builder()
                        .maskStringsWith(ValueMaskers.withRawValueFunction(value -> value.startsWith("\"secret:") ? "\"***\"" : value))
                        .build()
                )
                .build());

        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker, """
                {
                  "string": "maskMe",
                  "number": 12345,
                  "boolean": false,
                  "function": "mask [this secret] please",
                  "functionNull": "maskMe",
                  "functionConditional": {
                    "value1": "not a secret",
                    "value2": "secret: very much"
                  }
                }
                """, """
                {
                  "string": "***",
                  "number": "###",
                  "boolean": "&&&",
                  "function": "mask *** please",
                  "functionNull": null,
                  "functionConditional": {
                    "value1": "not a secret",
                    "value2": "***"
                  }
                }
                """);
    }

    @Test
    void shouldNotAllowGettingValuesOutsideOfIndex() {
        JsonMasker jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .maskKeys("customValueMasker", KeyMaskingConfig.builder()
                        .maskStringsWith(context -> {
                            assertThatThrownBy(() -> context.getByte(-1))
                                    .isInstanceOf(IndexOutOfBoundsException.class);
                            assertThatThrownBy(() -> context.getByte(context.byteLength()))
                                    .isInstanceOf(IndexOutOfBoundsException.class);

                            assertThatThrownBy(() -> context.replaceBytes(-1, 1, new byte[0], 1))
                                    .isInstanceOf(IndexOutOfBoundsException.class);
                            assertThatThrownBy(() -> context.replaceBytes(0, context.byteLength() + 1, new byte[0], 1))
                                    .isInstanceOf(IndexOutOfBoundsException.class);
                            assertThatThrownBy(() -> context.replaceBytes(1, context.byteLength(), new byte[0], 1))
                                    .isInstanceOf(IndexOutOfBoundsException.class);

                            assertThatThrownBy(() -> context.countNonVisibleCharacters(-1, 1))
                                    .isInstanceOf(IndexOutOfBoundsException.class);
                            assertThatThrownBy(() -> context.countNonVisibleCharacters(0, context.byteLength() + 1))
                                    .isInstanceOf(IndexOutOfBoundsException.class);
                            assertThatThrownBy(() -> context.countNonVisibleCharacters(1, context.byteLength()))
                                    .isInstanceOf(IndexOutOfBoundsException.class);
                        })
                        .build()
                )
                .build());
        jsonMasker.mask("""
                {
                  "customValueMasker": "maskMe"
                }
                """);
        jsonMasker.mask(new ByteArrayInputStream("""
                {
                  "customValueMasker": "maskMe"
                }
                """.getBytes(StandardCharsets.UTF_8)), new ByteArrayOutputStream());
    }

    @Test
    void maskEveryNumberDigitWithStringCharacter() {
        JsonMasker jsonMasker =
                JsonMasker.getMasker(
                        JsonMaskingConfig.builder()
                                .maskKeys("customValueMasker")
                                .maskNumbersWith(ValueMaskers.eachDigitWith("*"))
                                .build());
        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker,
                """
                        {
                          "customValueMasker": 0
                        }
                        """,
                """
                        {
                          "customValueMasker": "*"
                        }
                        """);
        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker,
                """
                        {
                          "customValueMasker": 123
                        }
                        """,
                """
                        {
                          "customValueMasker": "***"
                        }
                        """);
        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker,
                """
                        {
                          "customValueMasker": 12345
                        }
                        """,
                """
                        {
                          "customValueMasker": "*****"
                        }
                        """);
    }

    @Test
    void maskKeysWithSpecificConfigInAllowMode() {
        var jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .allowKeys("allowed")
                .allowJsonPaths(Set.of("$.allowedPath"))
                .maskKeys("maskedLikeEpsteinFiles", KeyMaskingConfig.builder()
                        .maskStringsWith("■■■■■")
                        .build())
                .maskJsonPaths("$.maskedPathLikeEpsteinFiles", KeyMaskingConfig.builder()
                        .maskStringsWith("■■■■■")
                        .build())
                .build());

        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker, """
                {
                  "allowed": "allowMe",
                  "masked": "maskMe",
                  "maskedLikeEpsteinFiles": "maskMe",
                  "allowedPath": "allowMe",
                  "maskedPath": "maskMe",
                  "maskedPathLikeEpsteinFiles": "maskMe"
                }
                """, """
                {
                  "allowed": "allowMe",
                  "masked": "***",
                  "maskedLikeEpsteinFiles": "■■■■■",
                  "allowedPath": "allowMe",
                  "maskedPath": "***",
                  "maskedPathLikeEpsteinFiles": "■■■■■"
                }
                """);
    }
}
