package dev.blaauwendraad.masker.json.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class Utf8UtilTest {

    @Test
    void asciiCharacters() {
        byte[] asciiBytes = {
            AsciiCharacter.ASTERISK,
            AsciiCharacter.BACK_SLASH,
            AsciiCharacter.CARRIAGE_RETURN,
            AsciiCharacter.COLON,
            AsciiCharacter.COMMA,
            AsciiCharacter.CURLY_BRACKET_CLOSE,
            AsciiCharacter.CURLY_BRACKET_OPEN,
            AsciiCharacter.DOUBLE_QUOTE,
            AsciiCharacter.HORIZONTAL_TAB,
            AsciiCharacter.LINE_FEED,
            AsciiCharacter.MINUS,
            AsciiCharacter.PERIOD,
            AsciiCharacter.PLUS,
            AsciiCharacter.SPACE,
            AsciiCharacter.SQUARE_BRACKET_OPEN,
            AsciiCharacter.SQUARE_BRACKET_CLOSE,
            AsciiCharacter.UPPERCASE_E,
            AsciiCharacter.LOWERCASE_A,
            AsciiCharacter.LOWERCASE_E,
            AsciiCharacter.LOWERCASE_F,
            AsciiCharacter.LOWERCASE_L,
            AsciiCharacter.LOWERCASE_N,
            AsciiCharacter.LOWERCASE_R,
            AsciiCharacter.LOWERCASE_S,
            AsciiCharacter.LOWERCASE_T,
            AsciiCharacter.LOWERCASE_U,
            AsciiCharacter.ZERO,
            AsciiCharacter.ONE,
            AsciiCharacter.TWO,
            AsciiCharacter.THREE,
            AsciiCharacter.FOUR,
            AsciiCharacter.FIVE,
            AsciiCharacter.SIX,
            AsciiCharacter.SEVEN,
            AsciiCharacter.EIGHT,
            AsciiCharacter.NINE
        };
        for (byte b : asciiBytes) {
            assertThat(Utf8Util.getCodePointByteLength(b)).isEqualTo(1);
        }
    }

    @Test
    void nonUtf8Byte() {
        byte b = (byte) 127;
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Utf8Util.getCodePointByteLength((byte) (b << 1)));
    }

    @Test
    void unicodeHexToChar() {
        Assertions.assertThat(Utf8Util.unicodeHexToChar((byte) '0', (byte) '0', (byte) '2', (byte) '0'))
                .isEqualTo(' ');
        Assertions.assertThat(Utf8Util.unicodeHexToChar((byte) '0', (byte) '0', (byte) '3', (byte) '0'))
                .isEqualTo('0');
        Assertions.assertThat(Utf8Util.unicodeHexToChar((byte) '0', (byte) '0', (byte) '4', (byte) '0'))
                .isEqualTo('@');
    }

    @Test
    void unicodeHexToCharInvalid() {
        Assertions.assertThatThrownBy(() -> Utf8Util.unicodeHexToChar((byte) 35, (byte) '0', (byte) '2', (byte) '0'))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid hex character '#'");

        Assertions.assertThatThrownBy(() -> Utf8Util.unicodeHexToChar((byte) 61, (byte) '0', (byte) '2', (byte) '0'))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid hex character '='");

        Assertions.assertThatThrownBy(() -> Utf8Util.unicodeHexToChar((byte) 71, (byte) '0', (byte) '2', (byte) '0'))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid hex character 'G'");

        Assertions.assertThatThrownBy(() -> Utf8Util.unicodeHexToChar((byte) 103, (byte) '0', (byte) '2', (byte) '0'))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid hex character 'g'");
    }

    @ParameterizedTest
    @MethodSource("unicodeCharactersLength")
    void unicodeCharacters(String character, int utf8ByteLength) {
        assertThat(Utf8Util.getCodePointByteLength(character.getBytes(StandardCharsets.UTF_8)[0]))
                .isEqualTo(utf8ByteLength);
    }

    @ParameterizedTest
    @MethodSource("nonVisibleCharactersInString")
    void nonVisibleCharactersInString(String value, int nonVisibleCharacters) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        assertThat(Utf8Util.countNonVisibleCharacters(bytes, 0, bytes.length)).isEqualTo(nonVisibleCharacters);
    }

    @ParameterizedTest
    @MethodSource("equivalentJsonNodes")
    void parsingObjectWithUtf8CharacterData(Set<String> equivalentJsonNodes) {
        JsonMapper jsonMapper = new JsonMapper();
        JsonNode jsonNode = null;
        for (String equivalentJsonNode : equivalentJsonNodes) {
            JsonNode otherJsonNode = jsonMapper.readTree(equivalentJsonNode);
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
                Arguments.of("\\\\", 1), // the value without Java escape is "\\", which is a JSON escape
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
                Arguments.of("d\\u001Eb\\u0018n9", 11));
    }

    private static Stream<Set<String>> equivalentJsonNodes() {
        return Stream.of(Set.of("\"\uD800\uDF48\"", "\"\\uD800\\uDF48\""), Set.of("""
                        {
                          "𐍈": "𐍈"
                        }
                        """, """
                        {
                          "\\uD800\\uDF48": "\\uD800\\uDF48"
                        }
                        """));
    }
}
