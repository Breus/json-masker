package masker;

public enum UTF8Encoding {
	ASTERISK((byte) 42),
	BACK_SLASH((byte) 92),
	CARRIAGE_RETURN((byte) 13),
	COLON((byte) 58),
	DOUBLE_QUOTE((byte) 34),
	HORIZONTAL_TAB((byte) 9),
	LINE_FEED((byte) 10),
	SPACE((byte) 32);

	final byte utf8ByteValue;

	UTF8Encoding(byte utf8ByteValue) {
		this.utf8ByteValue = utf8ByteValue;
	}

	public byte getUtf8ByteValue() {
		return utf8ByteValue;
	}
}