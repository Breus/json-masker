package dev.blaauwendraad.masker.json.path;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * The library represents a jsonpath as an array of segments.
 * See {@link JsonPathParser} for details.
 */
public record JsonPath(String[] segments) {

    /**
     * The last segment of the jsonpath key is an actual target key.
     * @return the last segment of the jsonpath key.
     */
    @Nullable
    public String getQueryArgument() {
        return segments.length != 0 ? segments[segments.length - 1] : null;
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
