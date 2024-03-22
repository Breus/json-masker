package dev.blaauwendraad.masker.json;

/**
 * {@link DescriptiveValueMasker} provides the description of what the implementation does.
 *
 * <p>In order to keep the API concise, the {@link ValueMasker} only requires the implementation
 * code, but for debugging and tests, it's convenient to know how each JSON value is supposed to be
 * masked.
 *
 * <p>As a result, when {@link dev.blaauwendraad.masker.json.config.KeyMaskingConfig} is serialized,
 * we can see the configuration as {@code maskNumbersWith="###"} instead of
 * {@code maskNumbersWith=dev.blaauwendraad.masker.json.ValueMaskers$$Lambda$425/0x000000080022e890@1039bfc4}
 *
 * @see ValueMaskers#describe(String, T)
 */
final class DescriptiveValueMasker<T extends ValueMasker> implements ValueMasker.AnyValueMasker {
    private final String description;
    private final T delegate;

    DescriptiveValueMasker(String description, T delegate) {
        this.description = description;
        this.delegate = delegate;
    }

    @Override
    public void maskValue(ValueMaskerContext context) {
        delegate.maskValue(context);
    }

    @Override
    public String toString() {
        return description;
    }
}
