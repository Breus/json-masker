package dev.blaauwendraad.masker.json.util;

/** ASCII encoding characters and utility methods. */
public final class AsciiCharacter {

    public static final byte ASTERISK = (byte) '*';
    public static final byte BACK_SLASH = (byte) '\\';
    public static final byte CARRIAGE_RETURN = (byte) 13;
    public static final byte COLON = (byte) ':';
    public static final byte COMMA = (byte) ',';
    public static final byte CURLY_BRACKET_CLOSE = (byte) '}';
    public static final byte CURLY_BRACKET_OPEN = (byte) '{';
    public static final byte DOUBLE_QUOTE = (byte) '"';
    public static final byte HORIZONTAL_TAB = (byte) 9;
    public static final byte LINE_FEED = (byte) 10;
    public static final byte MINUS = (byte) '-';
    public static final byte PERIOD = (byte) '.';
    public static final byte PLUS = (byte) '+';
    public static final byte SPACE = (byte) ' ';
    public static final byte SQUARE_BRACKET_OPEN = (byte) '[';
    public static final byte SQUARE_BRACKET_CLOSE = (byte) ']';
    public static final byte UPPERCASE_E = (byte) 'E';

    public static final byte LOWERCASE_A = (byte) 'a';
    public static final byte LOWERCASE_E = (byte) 'e';
    public static final byte LOWERCASE_F = (byte) 'f';
    public static final byte LOWERCASE_L = (byte) 'l';
    public static final byte LOWERCASE_N = (byte) 'n';
    public static final byte LOWERCASE_R = (byte) 'r';
    public static final byte LOWERCASE_S = (byte) 's';
    public static final byte LOWERCASE_T = (byte) 't';
    public static final byte LOWERCASE_U = (byte) 'u';

    public static final byte ZERO = (byte) '0';
    public static final byte ONE = (byte) '1';
    public static final byte TWO = (byte) '2';
    public static final byte THREE = (byte) '3';
    public static final byte FOUR = (byte) '4';
    public static final byte FIVE = (byte) '5';
    public static final byte SIX = (byte) '6';
    public static final byte SEVEN = (byte) '7';
    public static final byte EIGHT = (byte) '8';
    public static final byte NINE = (byte) '9';

    private AsciiCharacter() {
        /* don't instantiate */
    }

    /**
     * Tests if the given byte corresponds to a double quote '{@literal "}' in ASCII encoding.
     *
     * @param inputByte the input byte
     * @return {@code true} if the byte corresponds to a double quote in ASCII encoding, {@code false} otherwise
     */
    public static boolean isDoubleQuote(byte inputByte) {
        return DOUBLE_QUOTE == inputByte;
    }

    /**
     * Tests if the given byte corresponds to an escape character (backslash) '\' in ASCII encoding.
     *
     * @param inputByte the input byte
     * @return {@code true} if the byte corresponds to an escape character (backslash) in ASCII encoding, {@code false}
     *     otherwise
     */
    public static boolean isEscapeCharacter(byte inputByte) {
        return BACK_SLASH == inputByte;
    }

    /**
     * Tests if the given byte corresponds to a lowercase 'u' in ASCII encoding.
     *
     * @param inputByte the input byte
     * @return {@code true} if the byte corresponds to a lowercase 'u' in ASCII encoding, {@code false} otherwise
     */
    public static boolean isLowercaseU(byte inputByte) {
        return LOWERCASE_U == inputByte;
    }

    /**
     * Tests if the given byte corresponds to an opening square bracket '[' in ASCII encoding.
     *
     * @param inputByte the input byte
     * @return {@code true} if the byte corresponds to an opening square bracket '[' in ASCII encoding, {@code false}
     *     otherwise
     */
    public static boolean isSquareBracketOpen(byte inputByte) {
        return SQUARE_BRACKET_OPEN == inputByte;
    }

    /**
     * Tests if the given byte corresponds to a closing square bracket '{@literal ]}' in ASCII encoding.
     *
     * @param inputByte the input byte
     * @return {@code true} if the byte corresponds to a closing square bracket '{@literal ]}' in ASCII encoding,
     *     {@code false} otherwise
     */
    public static boolean isSquareBracketClose(byte inputByte) {
        return SQUARE_BRACKET_CLOSE == inputByte;
    }

    /**
     * Tests if the given byte corresponds to an opening curly bracket '{@literal {}}' in ASCII encoding.
     *
     * @param inputByte the input byte
     * @return {@code true} if the byte corresponds to an opening curly bracket '{@literal {}}' in ASCII encoding,
     *     {@code false} otherwise
     */
    public static boolean isCurlyBracketOpen(byte inputByte) {
        return CURLY_BRACKET_OPEN == inputByte;
    }

    /**
     * Tests if the given byte corresponds to a closing curly bracket '{@literal }}' in ASCII encoding.
     *
     * @param inputByte the input byte
     * @return {@code true} if the byte corresponds to a closing curly bracket '{@literal }}' in ASCII encoding,
     *     {@code false} otherwise
     */
    public static boolean isCurlyBracketClose(byte inputByte) {
        return CURLY_BRACKET_CLOSE == inputByte;
    }

    /**
     * Tests if the given byte corresponds to a lower case t 't' in ASCII encoding.
     *
     * @param inputByte the input byte
     * @return {@code true} if the byte corresponds to a lower case t 't' in ASCII encoding, {@code false} otherwise
     */
    public static boolean isLowercaseT(byte inputByte) {
        return LOWERCASE_T == inputByte;
    }
}
