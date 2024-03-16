package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

@FunctionalInterface
public interface ValueMasker {

    /**
     * Masks a target value with a static string value.
     * For example, {@literal "maskMe": "secret" -> "maskMe": "***"}.
     */
    static ValueMasker maskWith(String value) {
        String replacement = "\"" + value + "\"";
        byte[] replacementBytes = replacement.getBytes(StandardCharsets.UTF_8);
        return withDescription(
                replacement,
                context -> context.replaceValue(0, context.valueLength(), replacementBytes, 1)
        );
    }

    /**
     * Masks a target value with a static integer value.
     * For example, {@literal "maskMe": 12345 -> "maskMe": 0}.
     */
    static ValueMasker maskWith(int value) {
        byte[] replacementBytes = String.valueOf(value).getBytes(StandardCharsets.UTF_8);
        return withDescription(
                String.valueOf(value),
                context -> context.replaceValue(0, context.valueLength(), replacementBytes, 1)
        );
    }

    /**
     * Masks a target value with a static boolean value.
     * For example, {@literal "maskMe": true -> "maskMe": false}.
     */
    static ValueMasker maskWith(boolean value) {
        byte[] replacementBytes = String.valueOf(value).getBytes(StandardCharsets.UTF_8);
        return withDescription(
                String.valueOf(value),
                context -> context.replaceValue(0, context.valueLength(), replacementBytes, 1)
        );
    }

    /**
     * Masks all characters of a target string value with a static string value.
     * For example, {@literal "maskMe": "secret" -> "maskMe": "******"}.
     *
     * <p>Note: this implementation only replaces visible characters with a mask, meaning that JSON escape
     * character ('\') will not count towards the length of the masked value and the UTF-8 character data
     * ('{@code \}u1000'), including 4-byte UTF-8 characters ('{@code \}uD83D{@code \}uDCA9'), will only count as
     * a single character in the masked value.
     */
    static ValueMasker maskStringCharactersWith(String value) {
        byte[] replacementBytes = value.getBytes(StandardCharsets.UTF_8);
        String description = "every character as %s".formatted(value);
        return withDescription(description, context -> {
            /*
            So we don't add asterisks for escape characters or additional encoding bytes (which are not part of the String length)

            The actual length of the string is the length minus escape characters (which are not part of the
            string length). Also, unicode characters are denoted as 4-hex digits but represent actually
            just one character, so for each of them 3 asterisks should be removed.
             */
            int stringValueStart = 1; // skip the opening quote
            int stringValueLength = context.valueLength() - 2; // skip both quotes
            int nonVisibleCharacters = context.countNonVisibleCharacters(stringValueStart, stringValueLength);
            int maskLength = stringValueLength - nonVisibleCharacters;
            context.replaceValue(stringValueStart, stringValueLength, replacementBytes, maskLength);
        });
    }

    /**
     * Masks all digits of a target numeric value with a static digit.
     * For example, {@literal "maskMe": 12345 -> "maskMe": 88888}.
     */
    static ValueMasker maskNumberDigitsWith(int digit) {
        if (digit < 1 || digit > 9) {
            throw new IllegalArgumentException("Masking digit must be between 1 and 9 to avoid leading zeroes");
        }
        byte[] replacementBytes = String.valueOf(digit).getBytes(StandardCharsets.UTF_8);
        String description = "every digit as %s".formatted(digit);
        return withDescription(
                description,
                context -> context.replaceValue(0, context.valueLength(), replacementBytes, context.valueLength())
        );
    }

    /**
     * Does not mask a target value.
     *
     * @see KeyMaskingConfig.Builder#disableBooleanMasking()
     * @see KeyMaskingConfig.Builder#disableNumberMasking()
     */
    static ValueMasker noop() {
        return withDescription("<no masking>", context -> {
        });
    }

    /**
     * Masks a target string value (containing an email) while keeping some amount of the prefix characters and ability
     * to keep the domain unmasked.
     * For example,
     * {@literal "maskMe": "agavlyukovskiy@gmail.com" -> "maskMe": "***@gmail.com"}
     * {@literal "maskMe": "agavlyukovskiy@gmail.com" -> "maskMe": "ag***"}
     * {@literal "maskMe": "agavlyukovskiy@gmail.com" -> "maskMe": "ag***@gmail.com"}
     *
     * @param keepPrefixLength amount of prefix characters to keep unmasked
     * @param keepDomain if true - the email domain will remain unmasked
     * @param mask the static mask applied to the rest of the value
     */
    static ValueMasker maskEmail(int keepPrefixLength, boolean keepDomain, String mask) {
        byte[] replacementBytes = mask.getBytes(StandardCharsets.UTF_8);
        String description = "email, keep prefix: %s, keep domain: %s".formatted(keepPrefixLength, keepDomain);
        return withDescription(description, context -> {
            int suffixLength = 1; // keep closing quote
            if (keepDomain) {
                for (int i = 0; i < context.valueLength(); i++) {
                    if (context.getByte(i) == '@') {
                        suffixLength = context.valueLength() - i;
                    }
                }
            }
            int maskLength = context.valueLength() - keepPrefixLength - suffixLength;
            context.replaceValue(
                    keepPrefixLength + 1, // keep opening quote
                    maskLength - 1, // keep closing quote
                    replacementBytes,
                    1
            );
        });
    }

    /**
     * Masks a target value with a supplied {@link Function}. The target value is passed into the function as a string,
     * regardless of the type (string, numeric or a boolean), however any non-null returned value from the
     * function will always be a JSON string (with quotes added automatically).
     *
     * <p>Note: usually {@link ValueMasker} operates on a byte level without parsing JSON values into intermediate
     * objects, however this implementation will have to allocate a {@link String} before passing it into the function
     * and then turn it back into a byte array for replacement.
     */
    static ValueMasker maskWithStringFunction(Function<String, String> masker) {
        return withDescription("Function<String, String>", context -> {
            String value = context.asString();
            String maskedValue = masker.apply(value);
            if (maskedValue == null) {
                maskedValue = "null";
            } else {
                maskedValue = "\"" + maskedValue + "\"";
            }
            context.replaceValue(0, context.valueLength(), maskedValue.getBytes(StandardCharsets.UTF_8), 1);
        });
    }

    static ValueMasker withDescription(String description, ValueMasker delegate) {
        return new DescriptiveValueMasker(description, delegate);
    }

    void maskValue(ValueMaskerContext context);
}
