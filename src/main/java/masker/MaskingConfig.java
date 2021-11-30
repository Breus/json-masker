package masker;

public class MaskingConfig {
	public static MaskingConfig defaultConfig() { return custom().build(); }
	public static MaskingConfig.Builder custom() {
		return new MaskingConfig.Builder();
	}

	/**
	 * Specifies the fixed length of the mask when target value lengths should be obfuscated.
	 * <p>
	 * -1 means length obfuscation being disabled.
	 * <p>
	 * Default value: -1
	 */
	private int obfuscationLength = -1;

	/**
	 * Specifies the multi target key masking algorithm used to mask multiple be
	 * <p>
	 * {@link MultiTargetAlgorithm#SINGLE_TARGET_LOOP}   loops over the target key set and executes the single-target key masking algorithm for each key.
	 * (time complexity cN * M, where N is the message input size, M the target key set size, and c is some constant)
	 * {@link MultiTargetAlgorithm#KEYS_CONTAIN}         uses a dedicated multi-target algorithm by looking for a JSON key and checking whether the target key set contains this key.
	 * (time complexity cN, where N is the message input size and c is some constant)
	 * // TODO @breus: benchmark next statement
	 * Note: for small target key set, the {@link MultiTargetAlgorithm#SINGLE_TARGET_LOOP} might actually be faster for multi-target masking since the constant in is smaller.
	 * <p>
	 * Default value: KEYS_CONTAIN
	 */
	private enum MultiTargetAlgorithm {SINGLE_TARGET_LOOP, KEYS_CONTAIN}

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
