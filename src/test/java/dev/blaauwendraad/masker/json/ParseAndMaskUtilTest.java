package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

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
                JsonMaskingConfig.getDefault(Set.of("someSecret", "someSecret2"))
        );
        Assertions.assertEquals("*********", jsonNode.get("someSecret").textValue());
        Assertions.assertEquals("*****", jsonNode.get("someOtherKey").get("someSecret2").textValue());
        Assertions.assertEquals("hello", jsonNode.get("someOtherKey").get("noneSecret").textValue());
        Assertions.assertEquals(123, jsonNode.get("someOtherKey").get("numericKey").numberValue());
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
                JsonMaskingConfig.getDefault(Set.of("maskMe"))
        );
        JsonNode maskedNode = jsonNode.get("maskMe");
        Assertions.assertEquals("****", maskedNode.get("someOtherKey").textValue());
        Assertions.assertEquals("*********", maskedNode.get("someKey").textValue());
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
                JsonMaskingConfig.getDefault(Set.of("maskMe", "alsoMaskMe"))
        );
        JsonNode maskedNode = jsonNode.get("maskMe");
        Assertions.assertEquals("*****", maskedNode.get(0).textValue());
        Assertions.assertEquals("*****", maskedNode.get(1).textValue());
        Assertions.assertEquals("**", jsonNode.get("dontMaskMe").get(0).get("alsoMaskMe").textValue());
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
                JsonMaskingConfig.custom(Set.of("targetKey1", "targetKey2"), JsonMaskingConfig.TargetKeyMode.ALLOW)
                        .build()
        );
        Assertions.assertEquals(
                new ObjectMapper().readTree("\"c\\u0014x\"").asText(),
                jsonNode.get("&I").get("targetKey1").asText()
        );
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
                JsonMaskingConfig.custom(Set.of("targetKey1", "targetKey2"), JsonMaskingConfig.TargetKeyMode.ALLOW)
                        .build()
        );
        Assertions.assertEquals(
                new ObjectMapper().readTree("\"p\\u000FE\"").asText(),
                jsonNode.get("targetKey2").get("").asText()
        );
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
                JsonMaskingConfig.custom(Set.of("targetKey1", "targetKey2"), JsonMaskingConfig.TargetKeyMode.ALLOW)
                        .build()
        );
        Assertions.assertEquals(
                new ObjectMapper().readTree("\"\\r\\u0014\"").asText(),
                jsonNode.get("targetKey1").get(">").asText()
        );
    }
}
