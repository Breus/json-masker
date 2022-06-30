package masker;

public enum Utf8AsciiCharacter {
	ASTERISK((byte) 42),
	BACK_SLASH((byte) 92),
	CARRIAGE_RETURN((byte) 13),
	COLON((byte) 58),
	COMMA((byte) 44),
	CURLY_BRACKET_OPEN((byte) 123),
	DOUBLE_QUOTE((byte) 34),
	HORIZONTAL_TAB((byte) 9),
	LINE_FEED((byte) 10),
	LOWERCASE_E((byte) 101),
	MINUS((byte) 45),
	PERIOD((byte) 46),
	PLUS((byte) 43),
	SPACE((byte) 32),
	SQUARE_BRACKET_OPEN((byte) 91),
	UPPERCASE_E((byte) 69),

	LOWERCASE_F((byte) 102),
	LOWERCASE_N((byte) 110),
	LOWERCASE_T((byte) 116),

	ZERO((byte) 48),
	ONE((byte) 49),
	TWO((byte) 50),
	THREE((byte) 51),
	FOUR((byte) 52),
	FIVE((byte) 53),
	SIX((byte) 54),
	SEVEN((byte) 55),
	EIGHT((byte) 56),
	NINE((byte) 57);

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

	public static boolean isColon(byte inputByte) {
		return COLON.getUtf8ByteValue() == inputByte;
	}
}