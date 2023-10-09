package dev.blaauwendraad.masker.json.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

class Utf8UtilTest {

    @Test
    void asciiCharacters() {
        for (AsciiCharacter asciiCharacter : AsciiCharacter.values()) {
            Assertions.assertEquals(1, Utf8Util.getCodePointByteLength(asciiCharacter.getAsciiByteValue()));
        }
    }

    @ParameterizedTest
    @MethodSource("unicodeCharactersLength")
    void unicodeCharacters(String character, int utf8ByteLength) {
        Assertions.assertEquals(
                utf8ByteLength,
                Utf8Util.getCodePointByteLength(character.getBytes(StandardCharsets.UTF_8)[0])
        );
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
