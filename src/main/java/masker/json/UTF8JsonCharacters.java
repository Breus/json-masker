package masker.json;

import masker.UTF8Encoding;

public final class UTF8JsonCharacters {
    static byte[] whiteSpaceCharacters = new byte[]{UTF8Encoding.CARRIAGE_RETURN.getUtf8ByteValue(), UTF8Encoding.HORIZONTAL_TAB.getUtf8ByteValue(), UTF8Encoding.LINE_FEED.getUtf8ByteValue(), UTF8Encoding.SPACE.getUtf8ByteValue()};
    static byte[] numberCharacters = new byte[]{};

    private UTF8JsonCharacters() {
        // don't instantiate
    }

    // 0-9, -, +, E, e, .,
    static byte[] numbers() {
        return numberCharacters;
    }

    static byte[] whiteSpaces() {
        return whiteSpaceCharacters;
    }

    static boolean isWhiteSpace(byte utf8Character) {
        for (byte whiteSpaceChar : whiteSpaces()) {
            if (whiteSpaceChar == utf8Character) {
                return true;
            }
        }
        return false;
    }
}
