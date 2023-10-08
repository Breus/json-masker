package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ParseAndMaskUtilTest {
    @Test
    void parseAndMask() throws JsonProcessingException {
        String simpleJsonObjectAsString =
                "{\"someSecret\": \"someValue\", \n\"someOtherKey\": {\"someSecret2\": \"value\"}}";
        JsonNode jsonNode =
                ParseAndMaskUtil.parseStringAndMask(simpleJsonObjectAsString, "someSecret", new ObjectMapper());
        Assertions.assertEquals("*********", jsonNode.get("someSecret").textValue());
    }
}
