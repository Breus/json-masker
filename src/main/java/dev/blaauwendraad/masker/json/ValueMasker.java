package dev.blaauwendraad.masker.json;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * A functional interface for masking JSON values. Accepts {@link ValueMaskerContext} that contains
 * context of the current value being masked.
 *
 * @see ValueMaskers for out-of-the-box implementations
 */
@FunctionalInterface
public interface ValueMasker {
    /**
     * Used for masking JSON values. Accepts {@link ValueMaskerContext} that contains context of the
     * current value being masked.
     */
    void maskValue(ValueMaskerContext context);
}
