package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;
import dev.blaauwendraad.masker.json.util.ByteValueMaskerContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

class ValueMaskersTest {
    @Test
    void describe() {
        ValueMasker.AnyValueMasker valueMasker = context -> context.replaceBytes(0, context.byteLength(), "null".getBytes(StandardCharsets.UTF_8), 1);
        ValueMasker.AnyValueMasker descriptiveValueMasker = ValueMaskers.describe("null (literal)", valueMasker);

        Assertions.assertThat(valueMasker.toString())
                .startsWith("dev.blaauwendraad.masker.json.ValueMaskersTest$$Lambda$");
        Assertions.assertThat(descriptiveValueMasker.toString())
                .isEqualTo("null (literal)");
    }

    @Test
    void withStringValue() {
        var valueMasker = ValueMaskers.with("***");

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("secret", valueMasker))
                .isEqualTo("\"***\"");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("\"***\"");
        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(true, valueMasker))
                .isEqualTo("\"***\"");
    }

    @Test
    void withStringValueEncoded() {
        var valueMasker = ValueMaskers.with("\u0000\n");

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("secret", valueMasker))
                .isEqualTo("\"\\u0000\\n\"");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("\"\\u0000\\n\"");
        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(true, valueMasker))
                .isEqualTo("\"\\u0000\\n\"");
    }

    @Test
    void withIntegerValue() {
        var valueMasker = ValueMaskers.with(0);

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("secret", valueMasker))
                .isEqualTo("0");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("0");
        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(true, valueMasker))
                .isEqualTo("0");
    }

    @Test
    void withBooleanValue() {
        var valueMasker = ValueMaskers.with(false);

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("secret", valueMasker))
                .isEqualTo("false");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("false");
        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(true, valueMasker))
                .isEqualTo("false");
    }

    @Test
    void withNull() {
        var valueMasker = ValueMaskers.withNull();

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("secret", valueMasker))
                .isEqualTo("null");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("null");
        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(true, valueMasker))
                .isEqualTo("null");
    }

    @Test
    void eachCharacterWith() {
        var valueMasker = ValueMaskers.eachCharacterWith("*");

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("secret", valueMasker))
                .isEqualTo("\"******\"");
    }

    @Test
    void eachCharacterWithEncoded() {
        var valueMasker = ValueMaskers.eachCharacterWith("\u0000");

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("secret", valueMasker))
                .isEqualTo("\"\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\"");
    }

    @Test
    void eachDigitWithInteger() {
        var valueMasker = ValueMaskers.eachDigitWith(1);

        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("11111");

        Assertions.assertThatThrownBy(() -> ValueMaskers.eachDigitWith(0))
                .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> ValueMaskers.eachDigitWith(10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void eachDigitWithSingleCharacter() {
        var valueMasker = ValueMaskers.eachDigitWith("*");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("\"*****\"");
    }

    @Test
    void eachDigitWithSingleCharacterEncoded() {
        var valueMasker = ValueMaskers.eachDigitWith("\u0000");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("\"\\u0000\\u0000\\u0000\\u0000\\u0000\"");
    }

    @Test
    void eachDigitWithString() {
        var valueMasker = ValueMaskers.eachDigitWith("Nope");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("\"NopeNopeNopeNopeNope\"");
    }

    @Test
    void noop() {
        var valueMasker = ValueMaskers.noop();

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("secret", valueMasker))
                .isEqualTo("\"secret\"");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("12345");
        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(true, valueMasker))
                .isEqualTo("true");
    }

    @Test
    void email() {
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "agavlyukovskiy@gmail.com",
                        ValueMaskers.email(2, 2, true, "***")
                ))
                .isEqualTo("\"ag***iy@gmail.com\"");
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "agavlyukovskiy@gmail.com",
                        ValueMaskers.email(0, 2, true, "***")
                ))
                .isEqualTo("\"***iy@gmail.com\"");
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "agavlyukovskiy@gmail.com",
                        ValueMaskers.email(0, 0, true, "***")
                ))
                .isEqualTo("\"***@gmail.com\"");
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "agavlyukovskiy@gmail.com",
                        ValueMaskers.email(0, 0, false, "***")
                ))
                .isEqualTo("\"***\"");
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "agavlyukovskiy@gmail.com",
                        ValueMaskers.email(2, 0, false, "***")
                ))
                .isEqualTo("\"ag***\"");
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "agavlyukovskiy@gmail.com",
                        ValueMaskers.email(0, 2, false, "***")
                ))
                .isEqualTo("\"***om\"");
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "a@gmail.com",
                        ValueMaskers.email(2, 2, true, "***")
                ))
                .isEqualTo("\"a@gmail.com\"");
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "not-email",
                        ValueMaskers.email(2, 2, true, "***")
                ))
                .isEqualTo("\"no***il\"");
    }

    // Example IBANs from: https://www.iban.com/structure
    @Test
    void iban() {
        // Albania
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "AL35202111090000000001234567",
                        ValueMaskers.iban("**", 3)
                ))
                .isEqualTo("\"AL**2021**567\"");

        // Austria
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "AT483200000012345864",
                        ValueMaskers.iban("xx", 3)
                ))
                .isEqualTo("\"ATxx3200xx864\"");

        // Belgium
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "BE71096123456769",
                        ValueMaskers.iban("***", 4)
                ))
                .isEqualTo("\"BE***0961***6769\"");

        // Brazil
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "BR1500000000000010932840814P2",
                        ValueMaskers.iban("**", 2)
                ))
                .isEqualTo("\"BR**0000**P2\"");

        // Finland
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "FI2112349876543210",
                        ValueMaskers.iban("*", 4)
                ))
                .isEqualTo("\"FI*1234*3210\"");

        // France
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "FR7630006000011234567890189",
                        ValueMaskers.iban("**", 4)
                ))
                .isEqualTo("\"FR**3000**0189\"");

        // Germany
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "DE75512108001245126199",
                        ValueMaskers.iban("**", 4)
                ))
                .isEqualTo("\"DE**5121**6199\"");

        // Netherlands
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                    "NL91ABNA0417164300",
                    ValueMaskers.iban("***", 4)
                ))
                .isEqualTo("\"NL***ABNA***4300\"");

        // Norway
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "NO8330001234567",
                        ValueMaskers.iban("***", 4)
                ))
                .isEqualTo("\"NO***3000***4567\"");

        // Portugal
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "PT50002700000001234567833",
                        ValueMaskers.iban("!!", 1)
                ))
                .isEqualTo("\"PT!!0027!!3\"");


        // Luxembourg
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "LU120010001234567891",
                        ValueMaskers.iban("*", 0)
                ))
                .isEqualTo("\"LU*0010*\"");
    }

    @Test
    void withRawValueFunction() {
        var valueMasker = ValueMaskers.withRawValueFunction(value -> {
            if (value.startsWith("\"secret:")) {
                return "\"***\"";
            }
            if (value.startsWith("23")) {
                return "\"###\"";
            }
            if (value.equals("false")) {
                return "\"&&&\"";
            }
            return value;
        });

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("not a secret", valueMasker))
                .isEqualTo("\"not a secret\"");
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("secret: very much", valueMasker))
                .isEqualTo("\"***\"");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("12345");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(23456, valueMasker))
                .isEqualTo("\"###\"");
        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(true, valueMasker))
                .isEqualTo("true");
        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(false, valueMasker))
                .isEqualTo("\"&&&\"");
        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(true, ValueMaskers.withRawValueFunction(value -> null)))
                .isEqualTo("null");
    }

    @Test
    void withTextFunction() {
        var valueMasker = ValueMaskers.withTextFunction(value -> {
            if (value.startsWith("secret:")) {
                return "***";
            }
            if (value.startsWith("23")) {
                return "###";
            }
            if (value.equals("false")) {
                return "&&&";
            }
            return value;
        });

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("not a secret", valueMasker))
                .isEqualTo("\"not a secret\"");
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("secret: very much", valueMasker))
                .isEqualTo("\"***\"");
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("", valueMasker))
                .isEqualTo("\"\"");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("\"12345\"");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(23456, valueMasker))
                .isEqualTo("\"###\"");
        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(true, valueMasker))
                .isEqualTo("\"true\"");
        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(false, valueMasker))
                .isEqualTo("\"&&&\"");
        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(true, ValueMaskers.withTextFunction(value -> null)))
                .isEqualTo("null");
    }

    @Test
    void withTextFunctionEscapedCharacters() throws JsonProcessingException {
        // Verifying escaping per https://datatracker.ietf.org/doc/html/rfc8259#section-7
        // quotation mark
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("\\\"", ValueMaskers.withTextFunction(value -> {
            Assertions.assertThat(value).isEqualTo("\"");
            return value;
        }))).isEqualTo("\"\\\"\"");

        // reverse solidus
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("\\\\", ValueMaskers.withTextFunction(value -> {
            Assertions.assertThat(value).isEqualTo("\\");
            return value;
        }))).isEqualTo("\"\\\\\"");

        // the control characters (U+0000 through U+001F)
        ObjectMapper objectMapper = new ObjectMapper();
        for (int i = 0; i < 32; i++) {
            String controlCharacter = String.valueOf((char) i);
            String encoded = objectMapper.writeValueAsString(controlCharacter);

            Assertions.assertThat(ByteValueMaskerContext.maskStringWith(encoded.substring(1, encoded.length() - 1), ValueMaskers.withTextFunction(value -> {
                Assertions.assertThat(value).isEqualTo(controlCharacter);
                return value;
            }))).isEqualTo(encoded);
        }

        // forward slash, may be escaped on the input, but does not need to be escaped on the output
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("\\/", ValueMaskers.withTextFunction(value -> {
            Assertions.assertThat(value).isEqualTo("/");
            return value;
        }))).isEqualTo("\"/\"");
    }

    private static Stream<List<String>> unicodeCharacters() {
        // equivalent pairs of unicode characters: actual character (expected), JSON-escaped, Java-escaped
        return Stream.of(
                List.of("a", "\\u0061", "\u0061"),              // 1-byte character
                List.of("ƒ", "\\u0192", "\u0192"),              // 2-byte character
                List.of("€", "\\u20AC", "\u20AC"),              // 3-byte character
                List.of("䀀", "\\u4000", "\u4000"),              // 3-byte character
                List.of("𐍈", "\\uD800\\uDF48", "\uD800\uDF48"), // 4-byte character
                List.of("𠜎", "\\uD841\\uDF0E", "\uD841\uDF0E"), // 4-byte character
                List.of("💩", "\\uD83D\\uDCA9", "\uD83D\uDCA9")  // 4-byte character
        );
    }

    @ParameterizedTest
    @MethodSource("unicodeCharacters")
    void withTextFunctionUnicodeEncoded(List<String> characters) {
        String expected = characters.get(0);
        // equivalent pairs of unicode characters: JSON-escaped, Java-escaped, and actual character
        for (String unicodeCharacter : characters) {
            // single value
            Assertions.assertThat(ByteValueMaskerContext.maskStringWith(unicodeCharacter, ValueMaskers.withTextFunction(value -> {
                Assertions.assertThat(value).isEqualTo(expected);
                return value;
            }))).isEqualTo("\"" + expected + "\"");

            // lowercase hex value, isn't really allowed by JSON specification, but Java supports that in Character.digit
            // i.e. \\u20AC and \\u20ac both decoded to the same value €
            Assertions.assertThat(ByteValueMaskerContext.maskStringWith(unicodeCharacter.toLowerCase(), ValueMaskers.withTextFunction(value -> {
                Assertions.assertThat(value).isEqualTo(expected);
                return value;
            }))).isEqualTo("\"" + expected + "\"");

            // double value
            Assertions.assertThat(ByteValueMaskerContext.maskStringWith(unicodeCharacter + unicodeCharacter, ValueMaskers.withTextFunction(value -> {
                Assertions.assertThat(value).isEqualTo(expected + expected);
                return value;
            }))).isEqualTo("\"" + expected + expected + "\"");

            // with prefix
            Assertions.assertThat(ByteValueMaskerContext.maskStringWith("prefix" + unicodeCharacter, ValueMaskers.withTextFunction(value -> {
                Assertions.assertThat(value).isEqualTo("prefix" + expected);
                return value;
            }))).isEqualTo("\"prefix" + expected + "\"");

            // with suffix
            Assertions.assertThat(ByteValueMaskerContext.maskStringWith(unicodeCharacter + "suffix", ValueMaskers.withTextFunction(value -> {
                Assertions.assertThat(value).isEqualTo(expected + "suffix");
                return value;
            }))).isEqualTo("\"" + expected + "suffix\"");

            // with prefix and suffix
            Assertions.assertThat(ByteValueMaskerContext.maskStringWith("prefix" + unicodeCharacter + "suffix", ValueMaskers.withTextFunction(value -> {
                Assertions.assertThat(value).isEqualTo("prefix" + expected + "suffix");
                return value;
            }))).isEqualTo("\"prefix" + expected + "suffix\"");
        }
    }

    @Test
    void withTextFunctionInvalidEscape() {
        ValueMasker.AnyValueMasker valueMasker = ValueMaskers.withTextFunction(value -> value);

        Assertions.assertThatThrownBy(() -> ByteValueMaskerContext.maskStringWith("\\z", valueMasker))
                .isInstanceOf(InvalidJsonException.class)
                .hasMessage("Unexpected character after '\\': 'z' at index 3");
    }

    @Test
    void withTextFunctionInvalidUnicode() {
        ValueMasker.AnyValueMasker valueMasker = ValueMaskers.withTextFunction(value -> value);

        // high surrogate without low surrogate
        Assertions.assertThatThrownBy(() -> ByteValueMaskerContext.maskStringWith("\\uD83D", valueMasker))
                .isInstanceOf(InvalidJsonException.class)
                        .hasMessage("Invalid surrogate pair '\\uD83D' at index 1");

        // high surrogate followed by another high surrogate
        Assertions.assertThatThrownBy(() -> ByteValueMaskerContext.maskStringWith("\\uD83D\\uD83D", valueMasker))
                .isInstanceOf(InvalidJsonException.class)
                .hasMessage("Invalid surrogate pair '\\uD83D\\uD83D' at index 1");

        // high surrogate without low surrogate but other suffix
        Assertions.assertThatThrownBy(() -> ByteValueMaskerContext.maskStringWith("\\uD83Dsuffix", valueMasker))
                .isInstanceOf(InvalidJsonException.class)
                .hasMessage("Invalid surrogate pair '\\uD83D' at index 1");

        // high surrogate without low surrogate but an escape character
        Assertions.assertThatThrownBy(() -> ByteValueMaskerContext.maskStringWith("\\uD83D\\n0000", valueMasker))
                .isInstanceOf(InvalidJsonException.class)
                .hasMessage("Invalid surrogate pair '\\uD83D' at index 1");

        // low surrogate without high surrogate
        Assertions.assertThatThrownBy(() -> ByteValueMaskerContext.maskStringWith("\\uDCA9", valueMasker))
                .isInstanceOf(InvalidJsonException.class)
                .hasMessage("Invalid surrogate pair '\\uDCA9' at index 1");

        // low surrogate without high surrogate but other prefix
        Assertions.assertThatThrownBy(() -> ByteValueMaskerContext.maskStringWith("prefix\\uDCA9", valueMasker))
                .isInstanceOf(InvalidJsonException.class)
                .hasMessage("Invalid surrogate pair '\\uDCA9' at index 7");

        // unicode character uses lowercase hex value
        Assertions.assertThatThrownBy(() -> ByteValueMaskerContext.maskStringWith("\\uXXXX", valueMasker))
                .isInstanceOf(InvalidJsonException.class)
                .hasMessage("Invalid hex character 'X' at index 1");
    }

    @Test
    void customAnyValueMasker() {
        ValueMasker.AnyValueMasker valueMasker = context -> context.replaceBytes(0, context.byteLength(), "null".getBytes(StandardCharsets.UTF_8), 1);

        // checking that can be used with all types in the builder
        KeyMaskingConfig.builder()
                .maskStringsWith(valueMasker)
                .maskNumbersWith(valueMasker)
                .maskBooleansWith(valueMasker)
                .build();

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("secret", valueMasker))
                .isEqualTo("null");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("null");
        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(true, valueMasker))
                .isEqualTo("null");
    }

    @Test
    void customStringValueMasker() {
        ValueMasker.StringMasker valueMasker = context -> context.replaceBytes(0, context.byteLength(), "null".getBytes(StandardCharsets.UTF_8), 1);

        // checking that can be used with string types in the builder
        KeyMaskingConfig.builder()
                .maskStringsWith(valueMasker)
                .build();

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("secret", valueMasker))
                .isEqualTo("null");
    }

    @Test
    void customNumberValueMasker() {
        ValueMasker.NumberMasker valueMasker = context -> context.replaceBytes(0, context.byteLength(), "null".getBytes(StandardCharsets.UTF_8), 1);

        // checking that can be used with number types in the builder
        KeyMaskingConfig.builder()
                .maskNumbersWith(valueMasker)
                .build();

        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("null");
    }

    @Test
    void customBooleanValueMasker() {
        ValueMasker.BooleanMasker valueMasker = context -> context.replaceBytes(0, context.byteLength(), "null".getBytes(StandardCharsets.UTF_8), 1);

        // checking that can be used with boolean types in the builder
        KeyMaskingConfig.builder()
                .maskBooleansWith(valueMasker)
                .build();

        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(true, valueMasker))
                .isEqualTo("null");
    }

    @Test
    void maskEverySecondCharacter() {
        ValueMasker.StringMasker valueMasker =
                context -> context.replaceBytes(0, context.byteLength(), everyOtherCharacterMasked(context), 1);

        // checking that it can be used with string types in the builder
        KeyMaskingConfig.builder().maskStringsWith(valueMasker).build();

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("hello", valueMasker)).isEqualTo("\"h*l*o\"");
    }

    private byte[] everyOtherCharacterMasked(ValueMaskerContext valueMaskerContext) {
        byte[] maskResult = new byte[valueMaskerContext.byteLength()];
        maskResult[0] = '"';
        maskResult[maskResult.length - 1] = '"';
        for (int i = 1; i < valueMaskerContext.byteLength() - 1; i++) {
            if (i % 2 == 0) {
                maskResult[i] = '*';
            } else {
                maskResult[i] = valueMaskerContext.getByte(i);
            }
        }
        return maskResult;
    }
}

