package dev.blaauwendraad.masker.json.path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JsonPathTest {

    @Test
    void getLastSegment() {
        JsonPath jsonPath = new JsonPath(new String[]{"a", "b"});
        Assertions.assertEquals("b", jsonPath.getQueryArgument());
    }

    @Test
    void getLastSegmentForEmptyJsonPath() {
        JsonPath jsonPath = new JsonPath(new String[]{});
        Assertions.assertNull(jsonPath.getQueryArgument());
    }

    @Test
    void shouldCheckSegmentsOnEquals() {
        JsonPath a = new JsonPath(new String[]{"a", "b"});
        JsonPath b = new JsonPath(new String[]{"a", "b"});

        Assertions.assertEquals(a, a);
        Assertions.assertEquals(a, b);
        Assertions.assertEquals(a.hashCode(), b.hashCode());
        Assertions.assertNotEquals(a, null);
    }

}