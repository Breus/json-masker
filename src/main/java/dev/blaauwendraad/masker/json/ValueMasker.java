package dev.blaauwendraad.masker.json;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

@FunctionalInterface
public interface ValueMasker {

    static ValueMasker maskWith(String value) {
        String replacement = "\"" + value + "\"";
        byte[] replacementBytes = replacement.getBytes(StandardCharsets.UTF_8);
        return withDescription(
                replacement,
                context -> context.replaceValue(0, context.valueLength(), replacementBytes, 1)
        );
    }

    static ValueMasker maskWith(int value) {
        byte[] replacementBytes = String.valueOf(value).getBytes(StandardCharsets.UTF_8);
        return withDescription(
                String.valueOf(value),
                context -> context.replaceValue(0, context.valueLength(), replacementBytes, 1)
        );
    }

    static ValueMasker maskWith(boolean value) {
        byte[] replacementBytes = String.valueOf(value).getBytes(StandardCharsets.UTF_8);
        return withDescription(
                String.valueOf(value),
                context -> context.replaceValue(0, context.valueLength(), replacementBytes, 1)
        );
    }

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

    static ValueMasker noop() {
        return withDescription("<no masking>", context -> {
        });
    }

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

    static ValueMasker maskStringWithFunction(Function<String, String> masker) {
        return withDescription("Function<String, String>", context -> {
            String value = context.asString();
            String maskedValue = masker.apply(value);
            context.replaceValue(0, context.valueLength(), maskedValue.getBytes(StandardCharsets.UTF_8), 1);
        });
    }

    static ValueMasker withDescription(String description, ValueMasker delegate) {
        return new DescriptiveValueMasker(description, delegate);
    }

    void maskValue(ValueMaskerContext context);
}
