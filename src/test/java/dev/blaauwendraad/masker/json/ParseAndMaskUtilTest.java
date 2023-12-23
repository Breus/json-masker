package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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

//    @Test
//    void parseAndMaskInAllowMode() throws JsonProcessingException {
//        String simpleJsonObjectAsString = "{\"someSecret\": \"someValue\", \n\"someOtherKey\": {\"someSecret2\": \"value\"}, \"numericKey\": 123}";
//        JsonNode jsonNode = ParseAndMaskUtil.mask(
//                simpleJsonObjectAsString,
//                "someSecret",
//                JsonMaskingConfig.TargetKeyMode.ALLOW,
//                new ObjectMapper()
//        );
//        Assertions.assertEquals("\"someValue\"", jsonNode.get("someSecret").toString());
//        Assertions.assertEquals("\"*****\"", jsonNode.get("someOtherKey").get("someSecret2").toString());
//        Assertions.assertEquals("123", jsonNode.get("numericKey").toString());
//    }
}
