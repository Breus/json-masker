package dev.blaauwendraad.masker.json.path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JsonPathTest {

    @Test
    void getLastSegment() {
        JsonPath jsonPath = new JsonPath(new String[]{"a", "b"});
        Assertions.assertEquals("b", jsonPath.getLastSegment());
    }

    @Test
    void getLastSegmentForEmptyJsonPath() {
        JsonPath jsonPath = new JsonPath(new String[]{});
        Assertions.assertNull(jsonPath.getLastSegment());
    }

}