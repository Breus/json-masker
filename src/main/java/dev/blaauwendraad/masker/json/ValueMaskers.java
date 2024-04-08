package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;
import dev.blaauwendraad.masker.json.util.Utf8Util;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * Provides out-of-the-box implementations of {@link ValueMasker}.
 */
public final class ValueMaskers {
    private ValueMaskers() {
        // don't instantiate
    }

    /**
     * Provides information about the {@link ValueMasker} implementation. Which is useful for
     * debugging and testing purposes.
     *
     * @see DescriptiveValueMasker
     */
    @SuppressWarnings("unchecked")
    public static <T extends ValueMasker> T describe(String description, T delegate) {
        // descriptive masker is ValueMasker.AnyValueMasker and can be cast to any type
        // using the more restrictive type if supplied
        return (T) new DescriptiveValueMasker<>(description, delegate);
    }

    /**
     * Masks a target value with a static string value.
     * <p> For example, {@literal "maskMe": "secret" -> "maskMe": "***"}.
     */
    public static ValueMasker.AnyValueMasker with(String value) {
        String replacement = "\"" + value + "\"";
        byte[] replacementBytes = replacement.getBytes(StandardCharsets.UTF_8);
        return describe(
                replacement,
                context -> context.replaceBytes(0, context.byteLength(), replacementBytes, 1)
        );
    }

    /**
     * Masks a target value with a static integer value.
     * <p> For example, {@literal "maskMe": 12345 -> "maskMe": 0}.
     */
    public static ValueMasker.AnyValueMasker with(int value) {
        byte[] replacementBytes = String.valueOf(value).getBytes(StandardCharsets.UTF_8);
        return describe(
                String.valueOf(value),
                context -> context.replaceBytes(0, context.byteLength(), replacementBytes, 1)
        );
    }

    /**
     * Masks a target value with a static boolean value.
     * <p> For example, {@literal "maskMe": true -> "maskMe": false}.
     */
    public static ValueMasker.AnyValueMasker with(boolean value) {
        byte[] replacementBytes = String.valueOf(value).getBytes(StandardCharsets.UTF_8);
        return describe(
                String.valueOf(value),
                context -> context.replaceBytes(0, context.byteLength(), replacementBytes, 1)
        );
    }

    /**
     * Masks a target value with {@code null}.
     */
    public static ValueMasker.AnyValueMasker withNull() {
        byte[] replacementBytes = "null".getBytes(StandardCharsets.UTF_8);
        return describe(
                "null (literal)",
                context -> context.replaceBytes(0, context.byteLength(), replacementBytes, 1)
        );
    }

    /**
     * Masks all characters of a target string value with a static string value.
     * <p> For example, {@literal "maskMe": "secret" -> "maskMe": "******"}.
     *
     * <p> Note: this implementation only replaces visible characters with a mask, meaning that JSON
     * escape character ('\') will not count towards the length of the masked value and the unicode
     * characters ('{@code \}u1000'), including 4-byte UTF-8 characters ('{@code \}uD83D{@code
     * \}uDCA9'), will only count as a single character in the masked value.
     */
    public static ValueMasker.StringMasker eachCharacterWith(String value) {
        byte[] replacementBytes = value.getBytes(StandardCharsets.UTF_8);
        return describe(
                "every character as %s".formatted(value),
                context -> {
                    /*
                    So we don't add asterisks for escape characters or additional encoding bytes (which are not part of the String length)

                    The actual length of the string is the length minus escape characters (which are not part of the
                    string length). Also, unicode characters are denoted as 4-hex digits but represent actually
                    just one character, so for each of them 3 asterisks should be removed.
                     */
                    int stringValueStart = 1; // skip the opening quote
                    int stringValueLength = context.byteLength() - 2; // skip both quotes
                    int nonVisibleCharacters = context.countNonVisibleCharacters(stringValueStart, stringValueLength);
                    int maskLength = stringValueLength - nonVisibleCharacters;
                    context.replaceBytes(stringValueStart, stringValueLength, replacementBytes, maskLength);
                });
    }

    /**
     * Masks all digits of a target numeric value with a static digit.
     * <p> For example, {@literal "maskMe": 12345 -> "maskMe": 88888}.
     */
    public static ValueMasker.NumberMasker eachDigitWith(int digit) {
        if (digit < 1 || digit > 9) {
            throw new IllegalArgumentException(
                    "Masking digit must be between 1 and 9 to avoid leading zeroes which is invalid in JSON");
        }
        byte[] replacementBytes = String.valueOf(digit).getBytes(StandardCharsets.UTF_8);
        return describe(
                "every digit as integer: %s".formatted(digit),
                context -> context.replaceBytes(0, context.byteLength(), replacementBytes, context.byteLength())
        );
    }

    /**
     * Masks all digits of a target numeric value with a static String (which can also be a single
     * character).
     *
     * <p> For example, {@literal "maskMe": 12345 -> "maskMe": "*****"}.
     * <p> Or, for example {@literal "maskMe": 123 -> "maskMe": "NoNoNo"}.
     */
    public static ValueMasker.NumberMasker eachDigitWith(String value) {
        byte[] maskValueBytes = value.getBytes(StandardCharsets.UTF_8);
        int maskValueBytesLength = maskValueBytes.length;
        return describe(
                "every digit as string: %s".formatted(value),
                context -> {
                    int originalValueBytesLength = context.byteLength();
                    int totalMaskLength = originalValueBytesLength * maskValueBytesLength;
                    byte[] mask = new byte[2 + totalMaskLength]; // 2 for the opening and closing quotes
                    mask[0] = '\"';
                    for (int i = 0; i < totalMaskLength; i += maskValueBytesLength) {
                        for (int j = 0; j < maskValueBytesLength; j++) {
                            mask[1 + i + j] = maskValueBytes[j]; // 1 to step over the opening quote of the mask
                        }
                    }
                    mask[totalMaskLength + 1] = '\"';
                    context.replaceBytes(0, context.byteLength(), mask, 1);
                });
    }

    /**
     * Does not mask a target value (no-operation). Can be used if certain JSON value types do not
     * need to be masked, for example, not masking booleans or numbers.
     *
     * @see KeyMaskingConfig.Builder#maskBooleansWith(ValueMasker.BooleanMasker)
     * @see KeyMaskingConfig.Builder#maskNumbersWith(ValueMasker.NumberMasker)
     */
    public static ValueMasker.AnyValueMasker noop() {
        return describe("<no masking>", context -> {
        });
    }

    /**
     * Masks a target string value (containing an email) while keeping some number of the prefix or suffix
     * characters and the ability to keep the domain unmasked.
     * <p> For example:
     * <ul>
     *  <li>{@literal "maskMe": "agavlyukovskiy@gmail.com" -> "maskMe": "***@gmail.com"}</li>
     *  <li>{@literal "maskMe": "agavlyukovskiy@gmail.com" -> "maskMe": "ag***"}</li>
     *  <li>{@literal "maskMe": "agavlyukovskiy@gmail.com" -> "maskMe": "ag***@gmail.com"}</li>
     *  <li>{@literal "maskMe": "agavlyukovskiy@gmail.com" -> "maskMe": "ag***iy@gmail.com"}</li>
     * </ul>
     *
     * @param keepPrefixLength amount of prefix characters to keep unmasked
     * @param keepDomain       if true - the email domain will remain unmasked
     * @param mask             the static mask applied to the rest of the value
     */
    public static ValueMasker.StringMasker email(int keepPrefixLength, int keepSuffixLength, boolean keepDomain, String mask) {
        byte[] replacementBytes = mask.getBytes(StandardCharsets.UTF_8);
        return describe(
                "email, keep prefix: %s, keep suffix: %s, keep domain: %s"
                        .formatted(keepPrefixLength, keepSuffixLength, keepDomain),
                context -> {
                    int prefixLength = keepPrefixLength + 1; // add opening quote
                    int suffixLength = keepSuffixLength + 1; // keep closing quote
                    if (keepDomain) {
                        for (int i = 0; i < context.byteLength(); i++) {
                            if (context.getByte(i) == '@') {
                                // include domain in the suffix
                                suffixLength = context.byteLength() - i + keepSuffixLength;
                                break;
                            }
                        }
                    }
                    int maskLength = context.byteLength() - prefixLength - suffixLength;
                    if (maskLength > 0) {
                        context.replaceBytes(prefixLength, maskLength, replacementBytes, 1);
                    }
                });
    }

    /**
     * Masks a target value with the provided {@link Function}. The target value (as raw JSON literal) is passed into
     * the function as a string regardless of the JSON type (string, numeric or a boolean). In case the target value is
     * a JSON string the value the function will receive a JSON encoded value as it appears in the JSON, including
     * the opening and closing quotes, and the value containing escaped the control characters (e.g. {@code \n},
     * {@code \t}, etc.), quotation marks ({@code "}), escape character itself ({@code \}), and unicode-encoded
     * characters ({@code \}{@code uXXXX}).
     *
     * <p>Consequently, the return value of the provided function must be a valid JSON encoded literal (of any
     * JSON type), otherwise the masking will result in an invalid JSON.
     * If the return value is {@code null}, the target value will be replaced with {@code null} JSON literal.
     *
     * <p>It is strongly advised to use an equivalent function {@link ValueMaskers#withTextFunction(Function)}
     * which operates on a decoded string values and can never produce an invalid JSON.
     *
     * <p>The table below contains a couple examples for the masking
     * <table>
     *   <caption>Examples of using withRawValueFunction</caption>
     *   <tr>
     *     <th>Input JSON</th>
     *     <th>Function</th>
     *     <th>Masked JSON</th>
     *   <tr>
     *     <td>{@code { "maskMe": "a secret" }}
     *     <td>{@code value -> value.replaceAll("secret", "***")}
     *     <td>{@code { "maskMe": "a ***" }}
     *   <tr>
     *     <td>{@code { "maskMe": 12345 }}
     *     <td>{@code value -> value.startsWith("123") ? "0" : value}
     *     <td>{@code { "maskMe": 0 }}
     *   <tr>
     *     <td>{@code { "maskMe": "12345" }}
     *     <td>{@code value -> value.startsWith("123") ? "0" : value}
     *     <td>{@code { "maskMe": "12345" }}
     *   <tr>
     *     <td>{@code { "maskMe": "12345" }}
     *     <td>{@code value -> value.startsWith("\"123") ? "0" : value}
     *     <td>{@code { "maskMe": 0 }}
     *   <tr>
     *     <td>{@code { "maskMe": "secret" }}
     *     <td>{@code value -> "***"}
     *     <td>{@code { "maskMe": *** }} (invalid JSON)
     *   <tr>
     *     <td>{@code { "maskMe": "secret" }}
     *     <td>{@code value -> "\"***\""}
     *     <td>{@code { "maskMe": "***" }} (valid JSON)
     *   <tr>
     *     <td>{@code { "maskMe": "secret value" }}
     *     <td>{@code value -> value.substring(0, 3) + "***"}
     *     <td>{@code { "maskMe": "se*** }} (invalid JSON
     *   <tr>
     *     <td>{@code { "maskMe": "secret value" }}
     *     <td>{@code value -> value.startsWith("\"") ? value.substring(0, 4) + "***\"" : value}
     *     <td>{@code { "maskMe": "sec***" }} (valid JSON)
     *   <tr>
     *     <td>{@code { "maskMe": "Andrii \"Juice\" Pilshchykov" }}
     *     <td>{@code value -> value.replaceAll("\"", "(quote)")}
     *     <td>{@code { "maskMe": "Andrii \(quote)Juice\(quote) Pilshchykov" }} (invalid JSON)
     *   <tr>
     *     <td>{@code { "maskMe": "Andrii \"Juice\" Pilshchykov" }}
     *     <td>{@code value -> value.replaceAll("\\\"", "(quote)")}
     *     <td>{@code { "maskMe": "Andrii (quote)Juice(quote) Pilshchykov" }} (valid JSON)
     * </table>
     *
     * <p>Note: usually the {@link ValueMasker} operates on a byte level without parsing JSON values
     * into intermediate objects. This implementation, however,  needs to allocate a {@link String}
     * before passing it into the function and then turn it back into a byte array for the replacement,
     * which introduces some performance overhead.
     *
     * @see ValueMaskers#withTextFunction(Function)
     */
    public static ValueMasker.AnyValueMasker withRawValueFunction(Function<String, String> masker) {
        return describe(
                "withRawValueFunction (%s)".formatted(masker),
                context -> {
                    String value = context.asString(0, context.byteLength());
                    String maskedValue = masker.apply(value);
                    if (maskedValue == null) {
                        maskedValue = "null";
                    }
                    byte[] replacementBytes = maskedValue.getBytes(StandardCharsets.UTF_8);
                    context.replaceBytes(0, context.byteLength(), replacementBytes, 1);
                });
    }


    /**
     * Masks a target value by applying the provided {@link Function} on the textual representation of the original
     * value. The target is passed into the function as a string regardless of the JSON type (string, numeric or a
     * boolean). In case the original value was a JSON string, the {@link Function} will receive a decoded string value
     * without the quotes.
     *
     * <p>A non-null return value of the provided function will be encoded into a JSON string regardless of the
     * JSON type of the original value. Any character that MUST be escaped (as per RFC 8259, section 7) will be escaped.
     * Characters that MAY be escaped (as per RFC 8259) WILL NOT be escaped.
     * If the return value is {@code null}, the target value will be replaced with {@code null} JSON literal.
     *
     * <p>The table below contains a couple examples for the masking
     * <table>
     *   <caption>Examples of using withTextFunction</caption>
     *   <tr>
     *     <th>Input JSON</th>
     *     <th>Function</th>
     *     <th>Masked JSON</th>
     *   <tr>
     *     <td>{@code { "maskMe": "a secret" }}
     *     <td>{@code value -> value.replaceAll("secret", "***")}
     *     <td>{@code { "maskMe": "a ***" }}
     *   <tr>
     *     <td>{@code { "maskMe": 12345 }}
     *     <td>{@code value -> value.startsWith("123") ? "0" : value}
     *     <td>{@code { "maskMe": "0" }}
     *   <tr>
     *     <td>{@code { "maskMe": 12345 }}
     *     <td>{@code value -> value}
     *     <td>{@code { "maskMe": "12345" }}
     *   <tr>
     *     <td>{@code { "maskMe": "secret" }}
     *     <td>{@code value -> "***"}
     *     <td>{@code { "maskMe": "***" }}
     *   <tr>
     *     <td>{@code { "maskMe": "secret value" }}
     *     <td>{@code value -> value.substring(0, 3) + "***"}
     *     <td>{@code { "maskMe": "sec***" }}
     *   <tr>
     *     <td>{@code { "maskMe": "Andrii \"Juice\" Pilshchykov" }}
     *     <td>{@code value -> value.replaceAll("\"", "(quote)")}
     *     <td>{@code { "maskMe": "Andrii (quote)Juice(quote) Pilshchykov" }}
     * </table>
     *
     * <p>Note: in all other cases, the {@link ValueMasker} operates on a byte level without parsing JSON values into
     * intermediate objects. This implementation, however, needs to allocate a {@link String} before passing it to
     * the {@link Function} and then turn it back into a byte array for the replacement, which introduces some
     * performance overhead.
     */
    public static ValueMasker.AnyValueMasker withTextFunction(Function<String, String> masker) {
        return describe(
                "withTextFunction (%s)".formatted(masker),
                context -> {
                    String decodedValue; // the original value decoded
                    if (context.getByte(0) != '"') {
                        // deals with JSON numbers, booleans, and null
                        decodedValue = context.asString(0, context.byteLength());
                    } else {
                        // deals with JSON strings
                        int encodedIndex = 1; // skip opening quote of the JSON string
                        int valueEndIndex = context.byteLength() - 1; // minus the closing quote
                        int decodedIndex = 0; //
                        // the length of decodedBytes is guaranteed to be lower or equal to the length
                        // of the encoded bytes sequence:
                        // 1. for every encoded character (2 bytes), the output is the character without escape - 1 byte
                        // 2. for every unicode encoded character (6 bytes), the output character is within 1-3 bytes
                        // 3. for the pair of unicode encoded surrogates (12 bytes), the output is always 4 bytes
                        // which means that if any escape is present, the decodedBytes will have some null characters
                        // at the tail, which are cut from the resulting string
                        byte[] decodedBytes = new byte[context.byteLength()];
                        while (encodedIndex < valueEndIndex) {
                            byte originalByte = context.getByte(encodedIndex++);
                            // next character is escaped, removing the backslash
                            if (originalByte != '\\') {
                                decodedBytes[decodedIndex++] = originalByte; // unescaped character are already decoded
                            } else {
                                originalByte = context.getByte(encodedIndex++);
                                switch (originalByte) {
                                    case 'b' -> decodedBytes[decodedIndex++] = '\b';
                                    case 't' -> decodedBytes[decodedIndex++] = '\t';
                                    case 'n' -> decodedBytes[decodedIndex++] = '\n';
                                    case 'f' -> decodedBytes[decodedIndex++] = '\f';
                                    case 'r' -> decodedBytes[decodedIndex++] = '\r';
                                    case '"', '/', '\\' -> decodedBytes[decodedIndex++] = originalByte;
                                    case 'u' -> {
                                        // Decode hexadecimal encoded unicode character into
                                        int valueStartIndex = encodedIndex - 2;
                                        try {
                                            char unicodeHexBytesAsChar = Utf8Util.unicodeHexToChar(
                                                    context.getByte(encodedIndex++),
                                                    context.getByte(encodedIndex++),
                                                    context.getByte(encodedIndex++),
                                                    context.getByte(encodedIndex++)
                                            );
                                            if (unicodeHexBytesAsChar < 0x80) {
                                                // < 128 (in decimal) fits in 7 bits which is 1 byte of data in UTF-8
                                                decodedBytes[decodedIndex++] = (byte) unicodeHexBytesAsChar;
                                            } else if (unicodeHexBytesAsChar < 0x800) { // 2048 in decimal,
                                                // < 2048 (in decimal) fits in 11 bits which is 2 bytes of data in UTF-8
                                                decodedBytes[decodedIndex++] = (byte) (0xc0 | (unicodeHexBytesAsChar >> 6));
                                                decodedBytes[decodedIndex++] = (byte) (0x80 | (unicodeHexBytesAsChar & 0x3f));
                                            } else if (Character.isSurrogate(unicodeHexBytesAsChar)) {
                                                // decoding non-BMP characters in UTF-16 using a pair of high and low
                                                // surrogates which together form one unicode character.
                                                int codePoint = -1;
                                                if (Character.isHighSurrogate(unicodeHexBytesAsChar) // first surrogate must be the high surrogate
                                                        && encodedIndex < context.byteLength() - 6 /* -6 for all bytes of
                                                       the byte encoded unicode character (\\u + 4 hex bytes) to prevent possible ArrayIndexOutOfBoundsExceptions */
                                                        && context.getByte(encodedIndex) == '\\' // the high surrogate must be followed by a low surrogate (starting with \\u)
                                                        && context.getByte(encodedIndex + 1) == 'u'
                                                ) {
                                                    encodedIndex += 2; // step over the '\' and 'u'
                                                    char lowSurrogate = Utf8Util.unicodeHexToChar(
                                                            context.getByte(encodedIndex++),
                                                            context.getByte(encodedIndex++),
                                                            context.getByte(encodedIndex++),
                                                            context.getByte(encodedIndex++)
                                                    );
                                                    if (Character.isLowSurrogate(lowSurrogate)) {
                                                        codePoint = Character.toCodePoint(unicodeHexBytesAsChar, lowSurrogate);
                                                    }
                                                }
                                                if (codePoint < 0) {
                                                    // default String behaviour is to replace invalid surrogate pairs
                                                    // with the character '?', but from the JSON perspective,
                                                    // it's better to throw an InvalidJsonException
                                                    throw context.invalidJson("Invalid surrogate pair '%s'"
                                                            .formatted(context.asString(valueStartIndex, encodedIndex - valueStartIndex)), valueStartIndex);
                                                } else {
                                                    decodedBytes[decodedIndex++] = (byte) (0xf0 | (codePoint >> 18));
                                                    decodedBytes[decodedIndex++] = (byte) (0x80 | ((codePoint >> 12) & 0x3f));
                                                    decodedBytes[decodedIndex++] = (byte) (0x80 | ((codePoint >> 6) & 0x3f));
                                                    decodedBytes[decodedIndex++] = (byte) (0x80 | (codePoint & 0x3f));
                                                }
                                            } else {
                                                // dealing with characters with values between 2048 and 65536 which
                                                // equals to 2^16 or 16 bits, which is 3 bytes of data in UTF-8 encoding
                                                decodedBytes[decodedIndex++] = (byte) (0xe0 | (unicodeHexBytesAsChar >> 12));
                                                decodedBytes[decodedIndex++] = (byte) (0x80 | ((unicodeHexBytesAsChar >> 6) & 0x3f));
                                                decodedBytes[decodedIndex++] = (byte) (0x80 | (unicodeHexBytesAsChar & 0x3f));
                                            }
                                        } catch (IllegalArgumentException e) {
                                            throw context.invalidJson(e.getMessage(), valueStartIndex);
                                        }
                                    }
                                    default -> throw context.invalidJson("Unexpected character after '\\': '%s'".formatted((char) originalByte), encodedIndex);
                                }
                            }
                        }
                        decodedValue = new String(decodedBytes, 0, decodedIndex, StandardCharsets.UTF_8);
                    }
                    String maskedValue = masker.apply(decodedValue);
                    if (maskedValue == null) {
                        maskedValue = "null";
                    } else {
                        StringBuilder encoded = new StringBuilder();
                        encoded.append("\""); // opening quote of the encoded string
                        for (int i = 0; i < maskedValue.length(); i++) {
                            char character = maskedValue.charAt(i);
                            // escape all characters that need to be escaped, unicode character do not have to be
                            // transformed into \ u form
                            switch (character) {
                                case '\b' -> encoded.append("\\b");
                                case '\t' -> encoded.append("\\t");
                                case '\n' -> encoded.append("\\n");
                                case '\f' -> encoded.append("\\f");
                                case '\r' -> encoded.append("\\r");
                                case '"', '\\' -> encoded.append("\\").append(character);
                                default -> encoded.append(character);
                            }
                        }
                        encoded.append("\""); // closing quote of the encoded string
                        maskedValue = encoded.toString();
                    }
                    byte[] replacementBytes = maskedValue.getBytes(StandardCharsets.UTF_8);
                    context.replaceBytes(0, context.byteLength(), replacementBytes, 1);
                });
    }
}
