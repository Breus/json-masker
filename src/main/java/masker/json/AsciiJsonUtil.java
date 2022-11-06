package masker.json;

import static masker.AsciiCharacter.*;

public final class AsciiJsonUtil {
    static byte[] whiteSpaceCharacters = new byte[]{CARRIAGE_RETURN.getAsciiByteValue(),
                                                    HORIZONTAL_TAB.getAsciiByteValue(),
                                                    LINE_FEED.getAsciiByteValue(),
                                                    SPACE.getAsciiByteValue()};

    /**
     * Json number value can start with '-' or any digit 0-9
     */
    static byte[] firstNumberCharacters = new byte[]{MINUS.getAsciiByteValue(),
                                                     ZERO.getAsciiByteValue(),
                                                     ONE.getAsciiByteValue(),
                                                     TWO.getAsciiByteValue(),
                                                     THREE.getAsciiByteValue(),
                                                     FOUR.getAsciiByteValue(),
                                                     FIVE.getAsciiByteValue(),
                                                     SIX.getAsciiByteValue(),
                                                     SEVEN.getAsciiByteValue(),
                                                     EIGHT.getAsciiByteValue(),
                                                     NINE.getAsciiByteValue()};

    /**
     * Json numbers can include characters: '-', '+', '.', 'e', 'E', and digits 0-9
     */
    static byte[] numberCharacters = new byte[]{MINUS.getAsciiByteValue(),
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
                                                NINE.getAsciiByteValue()};

    static byte[] firstBooleanCharacters = new byte[]{LOWERCASE_F.getAsciiByteValue(), LOWERCASE_T.getAsciiByteValue()};

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
