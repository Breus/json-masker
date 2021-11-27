package masker;

public class MaskingConfig {
	public static MaskingConfig defaultConfig() { return custom().build(); }
	public static MaskingConfig.Builder custom() {
		return new MaskingConfig.Builder();
	}

	/**
	 * Specifies the fixed length of the mask when target value lengths should be obfuscated.
	 *
	 * -1 means length obfuscation being disabled.
	 */
	private int obfuscationLength = -1;

	int getObfuscationLength() {
		return obfuscationLength;
	}

	public static class Builder {
		private int obfuscationLength = -1;

		public Builder obfuscationLength(int obfuscationLength){
			this.obfuscationLength = obfuscationLength;
			return this;
		}

		public MaskingConfig build() {
			return new MaskingConfig(this);
		}
	}

	private MaskingConfig(Builder builder) {
		obfuscationLength = builder.obfuscationLength;
	}
}
