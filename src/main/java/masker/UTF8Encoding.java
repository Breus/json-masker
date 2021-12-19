package masker;

public enum UTF8Encoding {
	SPACE((byte) 32),
	DOUBLE_QUOTE((byte) 34),
	ASTERISK((byte) 42),
	COLON((byte) 58),
	LEFT_SQUARE_BRACKET((byte) 91),
	LEFT_CURLY_BRACKET((byte) 123);

	final byte utf8ByteValue;

	UTF8Encoding(byte utf8ByteValue) {
		this.utf8ByteValue = utf8ByteValue;
	}

	public byte getUtf8ByteValue() {
		return utf8ByteValue;
	}
}