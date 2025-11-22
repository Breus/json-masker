package dev.blaauwendraad.masker.json.util;

import dev.blaauwendraad.masker.randomgen.InvalidJsonPrettyPrinter;
import dev.blaauwendraad.masker.randomgen.RandomWhiteSpacePrettyPrinter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public enum JsonFormatter {
    PRETTY,
    COMPACT,
    RANDOM_WHITESPACE,
    INVALID_JSON;

    private final static int MAX_NUMBER_OF_SPACES_TO_INJECT = 50;

    private static final JsonMapper RANDOM_WHITESPACE_JSON_MAPPER = JsonMapper.builder().defaultPrettyPrinter(new RandomWhiteSpacePrettyPrinter(JsonFormatter.MAX_NUMBER_OF_SPACES_TO_INJECT)).build();
    private static final JsonMapper INVALID_JSON_MAPPER = JsonMapper.builder().defaultPrettyPrinter(new InvalidJsonPrettyPrinter()).build();

    public String format(JsonNode jsonNode) {
        return switch (this) {
            case PRETTY -> jsonNode.toPrettyString();
            case COMPACT -> jsonNode.toString();
            case RANDOM_WHITESPACE -> RANDOM_WHITESPACE_JSON_MAPPER.writeValueAsString(jsonNode);
            case INVALID_JSON -> INVALID_JSON_MAPPER.writeValueAsString(jsonNode);
        };
    }

    public boolean isValid() {
        return this != INVALID_JSON;
    }
}
