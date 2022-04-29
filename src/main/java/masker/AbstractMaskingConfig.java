package masker;

public abstract class AbstractMaskingConfig {
	/**
	 * Specifies the fixed length of the mask when target value lengths should be obfuscated.
	 * <p>
	 * -1 means length obfuscation is disabled.
	 * <p>
	 * Default value: -1 (via builder)
	 */
	private final int obfuscationLength;

	public int getObfuscationLength() {
		return obfuscationLength;
	}

	protected abstract static class Builder <T extends Builder<T>> {
		private int obfuscationLength = -1;

		public T obfuscationLength(int obfuscationLength){
			this.obfuscationLength = obfuscationLength;
			return self();
		}

		public abstract AbstractMaskingConfig build();

		// Subclasses must override this method to return "this"
		protected abstract T self();
	}

	protected AbstractMaskingConfig(Builder<?> builder) {
		obfuscationLength = builder.obfuscationLength;
	}
}
