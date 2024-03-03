package dev.blaauwendraad.masker.json.util;

import com.fasterxml.jackson.databind.JsonNode;
import dev.blaauwendraad.masker.randomgen.JsonPrettyPrinter;

import java.nio.charset.StandardCharsets;

public enum JsonFormatter {
    PRETTY,
    COMPACT,
    RANDOM_WHITESPACE;

    public String format(JsonNode jsonNode) {
        return switch (this) {
            case PRETTY -> jsonNode.toPrettyString();
            case COMPACT -> jsonNode.toString();
            case RANDOM_WHITESPACE -> {
                byte[] bytes = jsonNode.toString().getBytes(StandardCharsets.UTF_8);
                yield new String(new JsonPrettyPrinter(bytes).getWhiteSpaceInjectedJson(), StandardCharsets.UTF_8);
            }
        };
    }
}
