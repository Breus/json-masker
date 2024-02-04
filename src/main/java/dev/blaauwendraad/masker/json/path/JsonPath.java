package dev.blaauwendraad.masker.json.path;

import java.util.Arrays;

/**
 * Library's representation of JSONPath.
 * See {@link JsonPathParser} for details.
 */
public class JsonPath {
    private final String[] segments;

    JsonPath(String[] segments) {
        this.segments = segments;
    }

    public String getLastComponent() {
        return segments[segments.length-1];
    }

    @Override
    public String toString() {
        return String.join(".", segments);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JsonPath jsonPath = (JsonPath) o;
        return Arrays.equals(segments, jsonPath.segments);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(segments);
    }
}
