package dev.blaauwendraad.masker;

public enum AsciiCharacter {
    ASTERISK((byte) '*'),
    BACK_SLASH((byte) '\\'),
    CARRIAGE_RETURN((byte) 13),
    COLON((byte) ':'),
    COMMA((byte) ','),
    CURLY_BRACKET_OPEN((byte) '{'),
    DOUBLE_QUOTE((byte) '"'),
    HORIZONTAL_TAB((byte) 9),
    LINE_FEED((byte) 10),
    LOWERCASE_E((byte) 'e'),
    MINUS((byte) '-'),
    PERIOD((byte) '.'),
    PLUS((byte) '+'),
    SPACE((byte) ' '),
    SQUARE_BRACKET_OPEN((byte) '['),
    UPPERCASE_E((byte) 'E'),

    LOWERCASE_F((byte) 'f'),
    LOWERCASE_N((byte) 'n'),
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

    public static byte toAsciiByteValue(int digit) {
        return switch (digit) {
            case 0 -> ZERO.byteValue;
            case 1 -> ONE.byteValue;
            case 2 -> TWO.byteValue;
            case 3 -> THREE.byteValue;
            case 4 -> FOUR.byteValue;
            case 5 -> FIVE.byteValue;
            case 6 -> SIX.byteValue;
            case 7 -> SEVEN.byteValue;
            case 8 -> EIGHT.byteValue;
            case 9 -> NINE.byteValue;
            default -> throw new IllegalStateException("Unexpected digit: " + digit);
        };
    }

    public static boolean isDoubleQuote(byte inputByte) {
        return DOUBLE_QUOTE.getAsciiByteValue() == inputByte;
    }

    public static boolean isEscapeCharacter(byte inputByte) {
        return BACK_SLASH.getAsciiByteValue() == inputByte;
    }

    public static boolean isLowercaseU(byte inputByte) {
        return LOWERCASE_U.getAsciiByteValue() == inputByte;
    }

    public static boolean isColon(byte inputByte) {
        return COLON.getAsciiByteValue() == inputByte;
    }

    public byte getAsciiByteValue() {
        return byteValue;
    }
}