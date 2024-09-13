package dev.blaauwendraad.masker.json.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.blaauwendraad.masker.randomgen.InvalidJsonPrettyPrinter;
import dev.blaauwendraad.masker.randomgen.RandomWhiteSpacePrettyPrinter;

public enum JsonFormatter {
    PRETTY,
    COMPACT,
    RANDOM_WHITESPACE,
    INVALID_JSON;

    private final static int MAX_NUMBER_OF_SPACES_TO_INJECT = 50;

    @SuppressWarnings("ImmutableEnumChecker")
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String format(JsonNode jsonNode) {
        return switch (this) {
            case PRETTY -> jsonNode.toPrettyString();
            case COMPACT -> jsonNode.toString();
            case RANDOM_WHITESPACE -> withPrinter(jsonNode, new RandomWhiteSpacePrettyPrinter(JsonFormatter.MAX_NUMBER_OF_SPACES_TO_INJECT));
            case INVALID_JSON -> withPrinter(jsonNode, new InvalidJsonPrettyPrinter());
        };
    }

    public boolean isValid() {
        return this != INVALID_JSON;
    }

    private String withPrinter(JsonNode jsonNode, PrettyPrinter printer) {
        try {
            return objectMapper.writer(printer).writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
