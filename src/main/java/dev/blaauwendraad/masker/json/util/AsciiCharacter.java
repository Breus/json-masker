package dev.blaauwendraad.masker.json.util;

/** ASCII encoding characters and utility methods. */
public enum AsciiCharacter {
    ASTERISK((byte) '*'),
    BACK_SLASH((byte) '\\'),
    CARRIAGE_RETURN((byte) 13),
    COLON((byte) ':'),
    COMMA((byte) ','),
    CURLY_BRACKET_CLOSE((byte) '}'),
    CURLY_BRACKET_OPEN((byte) '{'),
    DOUBLE_QUOTE((byte) '"'),
    HORIZONTAL_TAB((byte) 9),
    LINE_FEED((byte) 10),
    MINUS((byte) '-'),
    PERIOD((byte) '.'),
    PLUS((byte) '+'),
    SPACE((byte) ' '),
    SQUARE_BRACKET_OPEN((byte) '['),
    SQUARE_BRACKET_CLOSE((byte) ']'),
    UPPERCASE_E((byte) 'E'),

    LOWERCASE_A((byte) 'a'),
    LOWERCASE_E((byte) 'e'),
    LOWERCASE_F((byte) 'f'),
    LOWERCASE_L((byte) 'l'),
    LOWERCASE_N((byte) 'n'),
    LOWERCASE_R((byte) 'r'),
    LOWERCASE_S((byte) 's'),
    LOWERCASE_T((byte) 't'),
    LOWERCASE_U((byte) 'u'),

    ZERO((byte) '0'),
    ONE((byte) '1'),
    TWO((byte) '2'),
    THREE((byte) '3'),
    FOUR((byte) '4'),
    FIVE((byte) '5'),
    SIX((byte) '6'),
    SEVEN((byte) '7'),
    EIGHT((byte) '8'),
    NINE((byte) '9');

    final byte byteValue;

    AsciiCharacter(byte byteValue) {
        this.byteValue = byteValue;
    }

    /**
     * Tests if the given byte corresponds to a double quote '{@literal "}' in ASCII encoding.
     *
     * @param inputByte the input byte
     * @return {@code true} if the byte corresponds to a double quote in ASCII encoding, {@code false} otherwise
     */
    public static boolean isDoubleQuote(byte inputByte) {
        return DOUBLE_QUOTE.getAsciiByteValue() == inputByte;
    }

    /**
     * Tests if the given byte corresponds to an escape character (backslash) '\' in ASCII encoding.
     *
     * @param inputByte the input byte
     * @return {@code true} if the byte corresponds to an escape character (backslash) in ASCII encoding, {@code false}
     * otherwise
     */
    public static boolean isEscapeCharacter(byte inputByte) {
        return BACK_SLASH.getAsciiByteValue() == inputByte;
    }

    /**
     * Tests if the given byte corresponds to a lowercase 'u' in ASCII encoding.
     *
     * @param inputByte the input byte
     * @return {@code true} if the byte corresponds to a lowercase 'u' in ASCII encoding, {@code false} otherwise
     */
    public static boolean isLowercaseU(byte inputByte) {
        return LOWERCASE_U.getAsciiByteValue() == inputByte;
    }

    /**
     * Tests if the given byte corresponds to an opening square bracket '[' in ASCII encoding.
     *
     * @param inputByte the input byte
     * @return {@code true} if the byte corresponds to an opening square bracket '[' in ASCII encoding, {@code false}
     * otherwise
     */
    public static boolean isSquareBracketOpen(byte inputByte) {
        return SQUARE_BRACKET_OPEN.getAsciiByteValue() == inputByte;
    }

    /**
     * Tests if the given byte corresponds to a closing square bracket '{@literal ]}' in ASCII encoding.
     *
     * @param inputByte the input byte
     * @return {@code true} if the byte corresponds to a closing square bracket '{@literal ]}' in ASCII encoding,
     * {@code false} otherwise
     */
    public static boolean isSquareBracketClose(byte inputByte) {
        return SQUARE_BRACKET_CLOSE.getAsciiByteValue() == inputByte;
    }

    /**
     * Tests if the given byte corresponds to an opening curly bracket '{@literal {}}' in ASCII
     * encoding.
     *
     * @param inputByte the input byte
     * @return {@code true} if the byte corresponds to an opening curly bracket '{@literal {}}' in ASCII
     *     encoding, {@code false} otherwise
     */
    public static boolean isCurlyBracketOpen(byte inputByte) {
        return CURLY_BRACKET_OPEN.getAsciiByteValue() == inputByte;
    }

    /**
     * Tests if the given byte corresponds to a closing curly bracket '{@literal }}' in ASCII
     * encoding.
     *
     * @param inputByte the input byte
     * @return {@code true} if the byte corresponds to a closing curly bracket '{@literal }}' in ASCII
     *     encoding, {@code false} otherwise
     */
    public static boolean isCurlyBracketClose(byte inputByte) {
        return CURLY_BRACKET_CLOSE.getAsciiByteValue() == inputByte;
    }

    /**
     * Tests if the given byte corresponds to a lower case t 't' in ASCII encoding.
     *
     * @param inputByte the input byte
     * @return {@code true} if the byte corresponds to a lower case t 't' in ASCII encoding, {@code false}
     *     otherwise
     */
    public static boolean isLowercaseT(byte inputByte) {
        return LOWERCASE_T.getAsciiByteValue() == inputByte;
    }

    /**
     * Get the corresponding ASCII encoding byte value.
     *
     * @return the ASCII encoding byte value.
     */
    public byte getAsciiByteValue() {
        return byteValue;
    }
}
