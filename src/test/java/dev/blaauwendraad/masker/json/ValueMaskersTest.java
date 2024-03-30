package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;
import dev.blaauwendraad.masker.json.util.ByteValueMaskerContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nonnull;
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
    void withTextFunctionEscapedCharacters() {
        String jsonEncoded = "\\b\\t\\n\\f\\r\\\"\\\\";
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(jsonEncoded, ValueMaskers.withTextFunction(value -> {
            Assertions.assertThat(value).isEqualTo("\b\t\n\f\r\"\\");
            return value;
        }))).isEqualTo("\"" + jsonEncoded + "\""); // needs to be escaped exactly like input

        String forwardSlash = "\\/";
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(forwardSlash, ValueMaskers.withTextFunction(value -> {
            Assertions.assertThat(value).isEqualTo("/");
            return value;
        }))).isEqualTo("\"/\""); // does not need to be escaped
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
        Assertions.assertThatThrownBy(() -> ByteValueMaskerContext.maskStringWith("\\z", ValueMaskers.withTextFunction(value -> value)))
                .isInstanceOf(InvalidJsonException.class)
                .hasMessageStartingWith("Unexpected character after '\\': 'z' at index 3");
    }

    @Test
    void withTextFunctionInvalidUnicode() {
        // high surrogate without low surrogate
        Assertions.assertThatThrownBy(() -> ByteValueMaskerContext.maskStringWith("\\uD83D", ValueMaskers.withTextFunction(value -> value)))
                .isInstanceOf(InvalidJsonException.class)
                        .hasMessageStartingWith("Invalid surrogate pair '\\uD83D', expected '\\uXXXX\\uXXXX' at index 1");

        // high surrogate followed by another high surrogate
        Assertions.assertThatThrownBy(() -> ByteValueMaskerContext.maskStringWith("\\uD83D\\uD83D", ValueMaskers.withTextFunction(value -> value)))
                .isInstanceOf(InvalidJsonException.class)
                .hasMessageStartingWith("Invalid surrogate pair '\\uD83D\\uD83D', expected '\\uXXXX\\uXXXX' at index 1");

        // high surrogate without low surrogate but other suffix
        Assertions.assertThatThrownBy(() -> ByteValueMaskerContext.maskStringWith("\\uD83Dsuffix", ValueMaskers.withTextFunction(value -> value)))
                .isInstanceOf(InvalidJsonException.class)
                .hasMessageStartingWith("Invalid surrogate pair '\\uD83D', expected '\\uXXXX\\uXXXX' at index 1");

        // high surrogate without low surrogate but an escape character
        Assertions.assertThatThrownBy(() -> ByteValueMaskerContext.maskStringWith("\\uD83D\n0000", ValueMaskers.withTextFunction(value -> value)))
                .isInstanceOf(InvalidJsonException.class)
                .hasMessageStartingWith("Invalid surrogate pair '\\uD83D', expected '\\uXXXX\\uXXXX' at index 1");

        // low surrogate without high surrogate
        Assertions.assertThatThrownBy(() -> ByteValueMaskerContext.maskStringWith("\\uDCA9", ValueMaskers.withTextFunction(value -> value)))
                .isInstanceOf(InvalidJsonException.class)
                .hasMessageStartingWith("Invalid surrogate pair '\\uDCA9', expected '\\uXXXX\\uXXXX' at index 1");

        // low surrogate without high surrogate but other prefix
        Assertions.assertThatThrownBy(() -> ByteValueMaskerContext.maskStringWith("prefix\\uDCA9", ValueMaskers.withTextFunction(value -> value)))
                .isInstanceOf(InvalidJsonException.class)
                .hasMessageStartingWith("Invalid surrogate pair '\\uDCA9', expected '\\uXXXX\\uXXXX' at index 7");
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

    @Nonnull
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

