package masker;

public final class Utf8Util {
    private Utf8Util() {
        // util
    }

    /**
     * UTF-8: variable width 1-4 byte code points:
     * 1 byte:  0xxxxxxx
     * 2 bytes: 110xxxxx 10xxxxxx
     * 3 bytes: 1110xxxx 10xxxxxx 10xxxxxx
     * 4 bytes: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
     *
     * @param input first (or only) code point byte
     * @return code point length in bytes
     */
    public static int getCodePointByteLength(byte input) {
        int shift = 7;
        int mostSignificantBitValue = input >> shift & 0x01;
        if (mostSignificantBitValue == 0) {
            return 1;
        } else {
            if ((input >> --shift & 0x01) == 0) {
                // this is a "following byte" (encoded as 10xxxxxx), so we will just return 0
                return 0;
            }
        }
        for (--shift; shift > 2; shift--) {
            int bitValue = input >> shift & 0x01;
            if (bitValue == 0) {
                return 7 - shift;
            }
        }
        throw new IllegalArgumentException("Input byte is not using UTF-8 encoding");
    }
}