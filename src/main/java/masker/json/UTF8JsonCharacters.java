package masker.json;

import masker.UTF8Encoding;

public final class UTF8JsonCharacters {
    private UTF8JsonCharacters() {
        // don't instantiate
    }

    static byte[] whiteSpaces() {
        return new byte[]{UTF8Encoding.CARRIAGE_RETURN.getUtf8ByteValue(), UTF8Encoding.HORIZONTAL_TAB.getUtf8ByteValue(), UTF8Encoding.LINE_FEED.getUtf8ByteValue(), UTF8Encoding.SPACE.getUtf8ByteValue()};
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
