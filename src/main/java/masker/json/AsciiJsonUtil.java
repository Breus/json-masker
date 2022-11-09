package masker.json;

import static masker.AsciiCharacter.CARRIAGE_RETURN;
import static masker.AsciiCharacter.EIGHT;
import static masker.AsciiCharacter.FIVE;
import static masker.AsciiCharacter.FOUR;
import static masker.AsciiCharacter.HORIZONTAL_TAB;
import static masker.AsciiCharacter.LINE_FEED;
import static masker.AsciiCharacter.LOWERCASE_E;
import static masker.AsciiCharacter.LOWERCASE_F;
import static masker.AsciiCharacter.LOWERCASE_T;
import static masker.AsciiCharacter.MINUS;
import static masker.AsciiCharacter.NINE;
import static masker.AsciiCharacter.ONE;
import static masker.AsciiCharacter.PERIOD;
import static masker.AsciiCharacter.PLUS;
import static masker.AsciiCharacter.SEVEN;
import static masker.AsciiCharacter.SIX;
import static masker.AsciiCharacter.SPACE;
import static masker.AsciiCharacter.THREE;
import static masker.AsciiCharacter.TWO;
import static masker.AsciiCharacter.UPPERCASE_E;
import static masker.AsciiCharacter.ZERO;

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

    static byte[] firstBooleanCharacters = new byte[] {
            LOWERCASE_F.getAsciiByteValue(),
            LOWERCASE_T.getAsciiByteValue()
    };

    private AsciiJsonUtil() {
        // don't instantiate
    }

    static byte[] firstNumberCharacters() {
        return firstNumberCharacters;
    }

    static byte[] firstBooleanCharacters() {
        return firstBooleanCharacters;
    }

    static byte[] numberCharacters() {
        return numberCharacters;
    }

    static byte[] whiteSpaces() {
        return whiteSpaceCharacters;
    }

    static boolean isWhiteSpace(byte utf8Character) {
        for (byte whiteSpaceChar : whiteSpaces()) {
            if (utf8Character == whiteSpaceChar) {
                return true;
            }
        }
        return false;
    }

    static boolean isFirstNumberChar(byte utf8Character) {
        for (byte firstNumberChar : firstNumberCharacters()) {
            if (utf8Character == firstNumberChar) {
                return true;
            }
        }
        return false;
    }

    static boolean isNumericCharacter(byte utf8Character) {
        for (byte numberChar : numberCharacters()) {
            if (utf8Character == numberChar) {
                return true;
            }
        }
        return false;
    }
}
