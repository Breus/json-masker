package dev.blaauwendraad.masker.json.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class Utf8UtilTest {

    @Test
    void asciiCharacters() {
        for (AsciiCharacter asciiCharacter : AsciiCharacter.values()) {
            assertThat(Utf8Util.getCodePointByteLength(asciiCharacter.getAsciiByteValue())).isEqualTo(1);
        }
    }

    @ParameterizedTest
    @MethodSource("unicodeCharactersLength")
    void unicodeCharacters(String character, int utf8ByteLength) {
        assertThat(Utf8Util.getCodePointByteLength(character.getBytes(StandardCharsets.UTF_8)[0])).isEqualTo(utf8ByteLength);
    }

    @ParameterizedTest
    @MethodSource("nonVisibleCharactersInString")
    void nonVisibleCharactersInString(String value, int nonVisibleCharacters) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        assertThat(Utf8Util.countNonVisibleCharacters(bytes, 0, bytes.length)).isEqualTo(nonVisibleCharacters);
    }

    @ParameterizedTest
    @MethodSource("equivalentJsonNodes")
    void parsingObjectWithUtf8CharacterData(Set<String> equivalentJsonNodes) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = null;
        for (String equivalentJsonNode : equivalentJsonNodes) {
            JsonNode otherJsonNode = objectMapper.readTree(equivalentJsonNode);
            if (jsonNode != null) {
                assertThat(otherJsonNode).isEqualTo(jsonNode);
            }
            jsonNode = otherJsonNode;
        }
    }

    private static Stream<Arguments> unicodeCharactersLength() {
        return Stream.of(
                Arguments.of("$", 1),
                Arguments.of("~", 1),
                Arguments.of("̀", 2),
                Arguments.of("€", 3),
                Arguments.of("†", 3),
                Arguments.of("䀀", 3),
                Arguments.of("\uD800\uDF48", 4),
                Arguments.of("𐍈", 4), // same as the above
                Arguments.of("\uD83D\uDCA9", 4),
                Arguments.of("💩", 4) // same as the above
        );
    }

    private static Stream<Arguments> nonVisibleCharactersInString() {
        return Stream.of(
                Arguments.of("$", 0),
                Arguments.of("~", 0),
                Arguments.of("̀", 1),
                Arguments.of("€", 2),
                Arguments.of("†", 2),
                Arguments.of("\u2020", 2), // same as the above
                // same as the above, but using UTF-8 string with character data so that Java doesn't convert it
                // into a single character
                Arguments.of("\\u2020", 5),
                Arguments.of("䀀", 2),
                Arguments.of("\uD800\uDF48", 3),
                Arguments.of("𐍈", 3), // same as the above
                // same as the above, but using UTF-8 string with character data so that Java doesn't convert it
                // into a single character
                Arguments.of("\\uD800\\uDF48", 10),
                Arguments.of("\uD83D\uDCA9", 3),
                Arguments.of("💩", 3), // same as the above
                Arguments.of("d\u001Eb\u0018n9", 1),
                // same as the above, but using UTF-8 string with character data so that Java doesn't convert it
                // into a single character
                Arguments.of("d\\u001Eb\\u0018n9", 11)
        );
    }

    private static Stream<Set<String>> equivalentJsonNodes() {
        return Stream.of(
                Set.of("\"\uD800\uDF48\"", "\"\\uD800\\uDF48\""),
                Set.of("""
                        {
                          "𐍈": "𐍈"
                        }
                        """,
                        """
                        {
                          "\\uD800\\uDF48": "\\uD800\\uDF48"
                        }
                        """)
        );
    }
}
