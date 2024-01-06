package dev.blaauwendraad.masker.json.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
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

    private static Stream<Arguments> unicodeCharactersLength() {
        return Stream.of(
                Arguments.of("$", 1),
                Arguments.of("~", 1),
                Arguments.of("\u0300", 2),
                Arguments.of("€", 3),
                Arguments.of("\u2020", 3),
                Arguments.of("\u4000", 3),
                Arguments.of("\uD800\uDF48", 4)
        );
    }
}
