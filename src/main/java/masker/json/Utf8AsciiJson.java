package masker.json;

import static masker.Utf8AsciiCharacter.*;

public final class Utf8AsciiJson {
    static byte[] whiteSpaceCharacters = new byte[]{
            CARRIAGE_RETURN.getUtf8ByteValue(),
            HORIZONTAL_TAB.getUtf8ByteValue(),
            LINE_FEED.getUtf8ByteValue(),
            SPACE.getUtf8ByteValue()
    };

    /**
     * Json number value can start with '-' or any digit 0-9
     */
    static byte[] firstNumberCharacters = new byte[]{
            MINUS.getUtf8ByteValue(),
            ZERO.getUtf8ByteValue(),
            ONE.getUtf8ByteValue(),
            TWO.getUtf8ByteValue(),
            THREE.getUtf8ByteValue(),
            FOUR.getUtf8ByteValue(),
            FIVE.getUtf8ByteValue(),
            SIX.getUtf8ByteValue(),
            SEVEN.getUtf8ByteValue(),
            EIGHT.getUtf8ByteValue(),
            NINE.getUtf8ByteValue()
    };

    /**
     * Json numbers can include characters: '-', '+', '.', 'e', 'E', and digits 0-9
     */
    static byte[] numberCharacters = new byte[] {
            MINUS.getUtf8ByteValue(),
            PLUS.getUtf8ByteValue(),
            PERIOD.getUtf8ByteValue(),
            LOWERCASE_E.getUtf8ByteValue(),
            UPPERCASE_E.getUtf8ByteValue(),
            ZERO.getUtf8ByteValue(),
            ONE.getUtf8ByteValue(),
            TWO.getUtf8ByteValue(),
            THREE.getUtf8ByteValue(),
            FOUR.getUtf8ByteValue(),
            FIVE.getUtf8ByteValue(),
            SIX.getUtf8ByteValue(),
            SEVEN.getUtf8ByteValue(),
            EIGHT.getUtf8ByteValue(),
            NINE.getUtf8ByteValue()
    };

    static byte[] firstBooleanCharacters = new byte[] {
            LOWERCASE_F.getUtf8ByteValue(),
            LOWERCASE_T.getUtf8ByteValue()
    };

    private Utf8AsciiJson() {
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
