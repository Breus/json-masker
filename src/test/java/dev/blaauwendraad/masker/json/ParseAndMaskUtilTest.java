package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class ParseAndMaskUtilTest {
    @Test
    void parseAndMaskStrings() throws JsonProcessingException {
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
                JsonMaskingConfig.builder().maskKeys("someSecret", "someSecret2").build()
        );
        assertThat(jsonNode.get("someSecret").textValue()).isEqualTo("***");
        assertThat(jsonNode.get("someOtherKey").get("someSecret2").textValue()).isEqualTo("***");
        assertThat(jsonNode.get("someOtherKey").get("noneSecret").textValue()).isEqualTo("hello");
        assertThat(jsonNode.get("someOtherKey").get("numericKey").numberValue()).isEqualTo(123);
    }

    @Test
    void parseAndMaskObjectValue() throws JsonProcessingException {
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
        assertThat(maskedNode.get("someOtherKey").textValue()).isEqualTo("***");
        assertThat(maskedNode.get("someKey").textValue()).isEqualTo("***");
    }

    @Test
    void parseAndMaskArrayValue() throws JsonProcessingException {
        JsonNode jsonNode = ParseAndMaskUtil.mask(
                """
                  {
                     "maskMe": ["hello", "there"],
                     "dontMaskMe": [{"alsoMaskMe": "no"}]
                  }
                """,
                JsonMaskingConfig.builder().maskKeys("maskMe", "alsoMaskMe").build()
        );
        JsonNode maskedNode = jsonNode.get("maskMe");
        assertThat(maskedNode.get(0).textValue()).isEqualTo("***");
        assertThat(maskedNode.get(1).textValue()).isEqualTo("***");
        assertThat(jsonNode.get("dontMaskMe").get(0).get("alsoMaskMe").textValue()).isEqualTo("***");
    }

    @Test
    void parseAndMaskAllowMode() throws JsonProcessingException {
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
                JsonMaskingConfig.builder().allowKeys("targetKey1", "targetKey2").build()
        );
        assertThat(jsonNode.get("&I").get("targetKey1").asText()).isEqualTo(new ObjectMapper().readTree("\"c\\u0014x\"").asText());
    }

    @Test
    void parseAndMaskAllowModeNestedField() throws JsonProcessingException {
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
                JsonMaskingConfig.builder().allowKeys("targetKey1", "targetKey2").build()
        );
        assertThat(jsonNode.get("targetKey2").get("").asText()).isEqualTo(new ObjectMapper().readTree("\"p\\u000FE\"").asText());
    }

    @Test
    void parseAndMaskAllowModeNestedField2() throws JsonProcessingException {
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
                JsonMaskingConfig.builder().allowKeys("targetKey1", "targetKey2").build()
        );
        assertThat(jsonNode.get("targetKey1").get(">").asText()).isEqualTo(new ObjectMapper().readTree("\"\\r\\u0014\"").asText());
    }
}
