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
        // TODO: can't mask numbers now
        Assertions.assertEquals("123", jsonNode.get("numericKey").toString());
    }
}
