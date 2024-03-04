package dev.blaauwendraad.masker.json.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.blaauwendraad.masker.randomgen.RandomWhiteSpacePrettyPrinter;

public enum JsonFormatter {
    PRETTY,
    COMPACT,
    RANDOM_WHITESPACE;

    private final static int MAX_NUMBER_OF_SPACES_TO_INJECT = 50;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String format(JsonNode jsonNode) {
        return switch (this) {
            case PRETTY -> jsonNode.toPrettyString();
            case COMPACT -> jsonNode.toString();
            case RANDOM_WHITESPACE -> getRandomWhiteSpaceJson(jsonNode, MAX_NUMBER_OF_SPACES_TO_INJECT);
        };
    }

    private String getRandomWhiteSpaceJson(JsonNode jsonNode, int maxNumberOfWhiteSpacesToInject) {
        RandomWhiteSpacePrettyPrinter prettyPrinter = new RandomWhiteSpacePrettyPrinter(maxNumberOfWhiteSpacesToInject);
        try {
            return objectMapper.writer(prettyPrinter).writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
