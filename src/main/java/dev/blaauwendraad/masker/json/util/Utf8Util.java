package dev.blaauwendraad.masker.json.util;

/** UTF-8 encoding utilities class */
public final class Utf8Util {
    private Utf8Util() {
        // util
    }

    /**
     * UTF-8: variable width 1-4 byte code points: 1 byte: 0xxxxxxx 2 bytes: 110xxxxx 10xxxxxx 3
     * bytes: 1110xxxx 10xxxxxx 10xxxxxx 4 bytes: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
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

    /**
     * Counts the number of non-visible characters inside the string. The intervals provided must be
     * within a single string as this method will not do boundary checks or terminate at the end of
     * string value.
     *
     * @param message the byte array containing the string
     * @param fromIndex the starting index of the string value (after the quote)
     * @param length the length of the string value (excluding the quotes)
     * @return the number of non-visible characters in the string, i.e., escape characters, unicode
     *     characters ('\u0000'), or other characters that are represented by more than a single
     *     byte are counted as one character
     */
    public static int countNonVisibleCharacters(byte[] message, int fromIndex, int length) {
        int index = fromIndex;
        int toIndex = fromIndex + length;
        boolean isEscapeCharacter = false;
        int nonVisibleCharacterCount = 0;
        while (index < toIndex) {
            byte currentByte = message[index];
            if (isEscapeCharacter) {
                /*
                 * Non-escaped backslashes are escape characters and are not actually part of the string but
                 * only used for character encoding, so must not be included in the mask.
                 */
                nonVisibleCharacterCount++;
                if (AsciiCharacter.isLowercaseU(currentByte)) {
                    /*
                     * The next 4 characters are hexadecimal digits which form a single character and are only
                     * there for encoding, so must not be included in the mask.
                     */
                    nonVisibleCharacterCount += 4;
                    index += 4;
                }
            } else {
                int codePointByteLength = Utf8Util.getCodePointByteLength(currentByte);
                if (codePointByteLength > 1) {
                    /*
                     * We only support UTF-8, so whenever code points are encoded using multiple bytes this should
                     * be represented by a single asterisk and the additional bytes used for encoding need to be
                     * removed.
                     */
                    nonVisibleCharacterCount += codePointByteLength - 1;
                }
            }
            isEscapeCharacter = !isEscapeCharacter && AsciiCharacter.isEscapeCharacter(currentByte);
            index++;
        }
        return nonVisibleCharacterCount;
    }
}
