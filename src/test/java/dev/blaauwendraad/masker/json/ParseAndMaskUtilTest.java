package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class ParseAndMaskUtilTest {
    @Test
    void parseAndMask() throws JsonProcessingException {
        String simpleJsonObjectAsString =
                "{\"someSecret\": \"someValue\", \n\"someOtherKey\": {\"someSecret2\": \"value\"}, \"numericKey\": 123}";
        JsonNode jsonNode =
                ParseAndMaskUtil.mask(simpleJsonObjectAsString, "someSecret", JsonMaskingConfig.TargetKeyMode.MASK, new ObjectMapper());
        Assertions.assertEquals("\"*********\"", jsonNode.get("someSecret").toString());
        Assertions.assertEquals("\"value\"", jsonNode.get("someOtherKey").get("someSecret2").toString());
        Assertions.assertEquals("123", jsonNode.get("numericKey").toString());
    }

    @Test
    void parseAndMaskInAllowMode() throws JsonProcessingException {
        String simpleJsonObjectAsString =
                "{\"someSecret\": \"someValue\", \n\"someOtherKey\": {\"someSecret2\": \"value\"}, \"numericKey\": 123}";
        JsonNode jsonNode =
                ParseAndMaskUtil.mask(simpleJsonObjectAsString, "someSecret", JsonMaskingConfig.TargetKeyMode.ALLOW, new ObjectMapper());
        Assertions.assertEquals("\"someValue\"", jsonNode.get("someSecret").toString());
        Assertions.assertEquals("\"*****\"", jsonNode.get("someOtherKey").get("someSecret2").toString());
        Assertions.assertEquals("\"*****\"", jsonNode.get("numericKey").toString());
    }

    @Test
    void adhoctest() throws JsonProcessingException {
        Set<String> targetKeys = Set.of("targetKey1", "targetKey2");
        String simpleJsonObjectAsString =
                "{\n"
                        + "  \"targetKey4\" : [ {\n"
                        + "    \"\u008F\u008A-邆-\" : {\n"
                        + "      \"targetKey2\" : false,\n"
                        + "      \"\" : [ ]\n"
                        + "    },\n"
                        + "    \"targetKey1\" : \"\\u0017V\"\n"
                        + "  } ],\n"
                        + "  \"targetKey1\" : \"^\u0098\",\n"
                        + "  \"ⁿ\\u0016\" : \"\",\n"
                        + "  \"\\u001BĒ\u009E.g₁qb\" : \"\"\n"
                        + "}";
        String utilOutput =
                ParseAndMaskUtil.mask(simpleJsonObjectAsString, targetKeys, JsonMaskingConfig.TargetKeyMode.MASK, new ObjectMapper()).toPrettyString();
        JsonMasker keyContainsMasker = new KeyContainsMasker(JsonMaskingConfig.getDefault(targetKeys));
        String keyContainsOutput = keyContainsMasker.mask(simpleJsonObjectAsString);
        System.out.println("ParseAndMarkUtil: " + utilOutput);
        System.out.println("KeyContainsMasker: " + keyContainsOutput);
        Assertions.assertEquals(utilOutput, keyContainsOutput);
    }
}
