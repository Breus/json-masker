package dev.blaauwendraad.masker.json;

/**
 * A functional interface which enables masking JSON values (boolean, numbers, and strings) in almost every imaginable
 * way. Accepts {@link ValueMaskerContext} that contains the context of the current value being masked.
 *
 * <p>This is a sealed interface in order to make sure that correct JSON type is only masked with the implementation
 * that supports masking of that particular type of the value. There's also a special {@link ValueMasker.AnyValueMasker}
 * that can mask values of any JSON type.
 *
 * <p>Most of the out-of-the-box implementation are, in fact, instances of {@link ValueMasker.AnyValueMasker}, but some
 * concrete implementations can only mask values of the specific type: For strings it's {@link ValueMaskers#email(int,
 * int, boolean, String)} and {@link ValueMaskers#eachCharacterWith(String)}, for numbers
 * {@link ValueMaskers#eachDigitWith(int)}.
 *
 * @see ValueMaskers for several out-of-the-box implementations
 */
// Sealed interface from Java 17 onwards, reverted back to non-sealed to Java 11 - Java 16 release
public /* sealed */ interface ValueMasker {
    /**
     * Used for masking JSON values. Accepts {@link ValueMaskerContext} that contains context of the current value being
     * masked.
     */
    void maskValue(ValueMaskerContext context);

    /**
     * {@link ValueMasker} that can mask JSON string values.
     *
     * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskStringsWith(ValueMasker.StringMasker)
     */
    @FunctionalInterface
    interface StringMasker extends ValueMasker {}

    /**
     * {@link ValueMasker} that can mask JSON number values.
     *
     * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskNumbersWith(ValueMasker.NumberMasker)
     */
    @FunctionalInterface
    interface NumberMasker extends ValueMasker {}

    /**
     * {@link ValueMasker} that can mask JSON boolean values.
     *
     * @see dev.blaauwendraad.masker.json.config.KeyMaskingConfig.Builder#maskBooleansWith(ValueMasker.BooleanMasker)
     */
    @FunctionalInterface
    interface BooleanMasker extends ValueMasker {}

    /** {@link ValueMasker} that can mask any JSON value (string, number or a boolean). */
    @FunctionalInterface
    interface AnyValueMasker extends ValueMasker, StringMasker, NumberMasker, BooleanMasker {}
}
