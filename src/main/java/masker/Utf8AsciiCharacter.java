package masker;

public enum Utf8AsciiCharacter {
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

	final byte utf8ByteValue;

	Utf8AsciiCharacter(byte utf8ByteValue) {
		this.utf8ByteValue = utf8ByteValue;
	}

	public byte getUtf8ByteValue() {
		return utf8ByteValue;
	}

	public static byte toUtf8ByteValue(int digit) {
		return switch (digit) {
			case 0 -> ZERO.utf8ByteValue;
			case 1 -> ONE.utf8ByteValue;
			case 2 -> TWO.utf8ByteValue;
			case 3 -> THREE.utf8ByteValue;
			case 4 -> FOUR.utf8ByteValue;
			case 5 -> FIVE.utf8ByteValue;
			case 6 -> SIX.utf8ByteValue;
			case 7 -> SEVEN.utf8ByteValue;
			case 8 -> EIGHT.utf8ByteValue;
			case 9 -> NINE.utf8ByteValue;
			default -> throw new IllegalStateException("Unexpected value: " + digit);
		};
	}

	public static boolean isDoubleQuote(byte inputByte) {
		return DOUBLE_QUOTE.getUtf8ByteValue() == inputByte;
	}

	public static boolean isEscapeCharacter(byte inputByte) {
		return BACK_SLASH.getUtf8ByteValue() == inputByte;
	}

	public static boolean isLowercaseU(byte inputByte) {
		return LOWERCASE_U.getUtf8ByteValue() == inputByte;
	}

	public static boolean isColon(byte inputByte) {
		return COLON.getUtf8ByteValue() == inputByte;
	}
}