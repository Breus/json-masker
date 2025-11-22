package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

final class ParseAndMaskUtilTest {
    @Test
    void parseAndMaskStrings() {
        JsonNode jsonNode = ParseAndMaskUtil.mask(
                """
                          {
                              "someSecret": "someValue",
                              "someOtherKey": {
                                  "someSecret2": "value",
                                  "noneSecret": "hello",
                                  "numericKey": 123
                              }
                          }
                        """,
                JsonMaskingConfig.builder().maskKeys(Set.of("someSecret", "someSecret2")).build()
        );
        assertThat(jsonNode.get("someSecret").asString()).isEqualTo("***");
        assertThat(jsonNode.get("someOtherKey").get("someSecret2").asString()).isEqualTo("***");
        assertThat(jsonNode.get("someOtherKey").get("noneSecret").asString()).isEqualTo("hello");
        assertThat(jsonNode.get("someOtherKey").get("numericKey").numberValue()).isEqualTo(123);
    }

    @Test
    void parseAndMaskObjectValue() {
        JsonNode jsonNode = ParseAndMaskUtil.mask(
                """
                          {
                             "maskMe": {
                                  "someKey": "someValue",
                                  "someOtherKey": "yes1"
                             }
                          }
                        """,
                JsonMaskingConfig.builder().maskKeys("maskMe").build()
        );
        JsonNode maskedNode = jsonNode.get("maskMe");
        assertThat(maskedNode.get("someOtherKey").asString()).isEqualTo("***");
        assertThat(maskedNode.get("someKey").asString()).isEqualTo("***");
    }

    @Test
    void parseAndMaskArrayValue() {
        JsonNode jsonNode = ParseAndMaskUtil.mask("""
                          {
                             "maskMe": ["hello", "there"],
                             "dontMaskMe": [{"alsoMaskMe": "no"}]
                          }
                        """,
                JsonMaskingConfig.builder().maskKeys(Set.of("maskMe", "alsoMaskMe")).build()
        );
        JsonNode maskedNode = jsonNode.get("maskMe");
        assertThat(maskedNode.get(0).asString()).isEqualTo("***");
        assertThat(maskedNode.get(1).asString()).isEqualTo("***");
        assertThat(jsonNode.get("dontMaskMe").get(0).get("alsoMaskMe").asString()).isEqualTo("***");
    }

    @Test
    void parseAndMaskAllowMode() {
        JsonNode jsonNode = ParseAndMaskUtil.mask(
                """
                        {
                          "&I" : {
                            "\\u0001d" : [ ],
                            "targetKey1" : "c\\u0014x",
                            "" : {
                              "targetKey3" : null
                            },
                            "\\u0013f" : 8360372093959137846
                          },
                          "gn" : [ ],
                          "" : ""
                        }
                        """,
                JsonMaskingConfig.builder().allowKeys(Set.of("targetKey1", "targetKey2")).build()
        );
        assertThat(jsonNode.get("&I").get("targetKey1").asString()).isEqualTo(new JsonMapper().readTree("\"c\\u0014x\"").asString());
    }

    @Test
    void parseAndMaskAllowModeNestedField() {
        JsonNode jsonNode = ParseAndMaskUtil.mask(
                """
                        {
                          "r" : [ ],
                          "targetKey2" : {
                            "₁ရj" : null,
                            "" : "p\\u000FE",
                            "ဝ\\u0007" : 69282835180228295535962081231619267644
                          },
                          "targetKey1" : [ ]
                        }
                        """,
                JsonMaskingConfig.builder().allowKeys(Set.of("targetKey1", "targetKey2")).build()
        );
        assertThat(jsonNode.get("targetKey2").get("").asString()).isEqualTo(new JsonMapper().readTree("\"p\\u000FE\"").asString());
    }

    @Test
    void parseAndMaskAllowModeNestedField2() {
        JsonNode jsonNode = ParseAndMaskUtil.mask(
                """
                        {
                          "7" : 2872595827985929903,
                          "5" : "",
                          "targetKey1" : {
                            "fC" : { },
                            "" : { },
                            ">" : "\\r\\u0014",
                            "targetKey1" : null
                          }
                        }
                        """,
                JsonMaskingConfig.builder().allowKeys(Set.of("targetKey1", "targetKey2")).build()
        );
        assertThat(jsonNode.get("targetKey1").get(">").asString()).isEqualTo(new JsonMapper().readTree("\"\\r\\u0014\"").asString());
    }
}
