package dev.blaauwendraad.masker.json.util;

import static dev.blaauwendraad.masker.json.util.AsciiCharacter.CARRIAGE_RETURN;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.EIGHT;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.FIVE;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.FOUR;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.HORIZONTAL_TAB;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.LINE_FEED;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.LOWERCASE_E;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.MINUS;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.NINE;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.ONE;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.PERIOD;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.PLUS;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.SEVEN;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.SIX;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.SPACE;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.THREE;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.TWO;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.UPPERCASE_E;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.ZERO;

public final class AsciiJsonUtil {
    static byte[] whiteSpaceCharacters = new byte[] {
            CARRIAGE_RETURN.getAsciiByteValue(),
            HORIZONTAL_TAB.getAsciiByteValue(),
            LINE_FEED.getAsciiByteValue(),
            SPACE.getAsciiByteValue()
    };

    /**
     * Json number value can start with '-' or any digit 0-9
     */
    static byte[] firstNumberCharacters = new byte[] {
            MINUS.getAsciiByteValue(),
            ZERO.getAsciiByteValue(),
            ONE.getAsciiByteValue(),
            TWO.getAsciiByteValue(),
            THREE.getAsciiByteValue(),
            FOUR.getAsciiByteValue(),
            FIVE.getAsciiByteValue(),
            SIX.getAsciiByteValue(),
            SEVEN.getAsciiByteValue(),
            EIGHT.getAsciiByteValue(),
            NINE.getAsciiByteValue()
    };

    /**
     * Json numbers can include characters: '-', '+', '.', 'e', 'E', and digits 0-9
     */
    static byte[] numberCharacters = new byte[] {
            MINUS.getAsciiByteValue(),
            PLUS.getAsciiByteValue(),
            PERIOD.getAsciiByteValue(),
            LOWERCASE_E.getAsciiByteValue(),
            UPPERCASE_E.getAsciiByteValue(),
            ZERO.getAsciiByteValue(),
            ONE.getAsciiByteValue(),
            TWO.getAsciiByteValue(),
            THREE.getAsciiByteValue(),
            FOUR.getAsciiByteValue(),
            FIVE.getAsciiByteValue(),
            SIX.getAsciiByteValue(),
            SEVEN.getAsciiByteValue(),
            EIGHT.getAsciiByteValue(),
            NINE.getAsciiByteValue()
    };

    private AsciiJsonUtil() {
        // don't instantiate
    }

    static byte[] firstNumberCharacters() {
        return firstNumberCharacters;
    }

    static byte[] numberCharacters() {
        return numberCharacters;
    }

    static byte[] whiteSpaces() {
        return whiteSpaceCharacters;
    }

    public static boolean isWhiteSpace(byte utf8Character) {
        for (byte whiteSpaceChar : whiteSpaces()) {
            if (utf8Character == whiteSpaceChar) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFirstNumberChar(byte utf8Character) {
        for (byte firstNumberChar : firstNumberCharacters()) {
            if (utf8Character == firstNumberChar) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNumericCharacter(byte utf8Character) {
        for (byte numberChar : numberCharacters()) {
            if (utf8Character == numberChar) {
                return true;
            }
        }
        return false;
    }
}
