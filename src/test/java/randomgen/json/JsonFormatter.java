package randomgen.json;

import com.fasterxml.jackson.databind.JsonNode;

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
                yield new String(new RandomJsonWhiteSpaceInjector(bytes, 50).getWhiteSpaceInjectedJson(), StandardCharsets.UTF_8);
            }
        };
    }
}
