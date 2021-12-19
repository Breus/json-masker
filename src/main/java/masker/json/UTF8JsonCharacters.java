package masker.json;

import masker.UTF8Encoding;

public class UTF8JsonCharacters {
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
