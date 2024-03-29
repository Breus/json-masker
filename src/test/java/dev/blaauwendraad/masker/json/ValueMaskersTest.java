package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;
import dev.blaauwendraad.masker.json.util.ByteValueMaskerContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;

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
            return value;
        });

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("not a secret", valueMasker))
                .isEqualTo("\"not a secret\"");
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("secret: very much", valueMasker))
                .isEqualTo("\"***\"");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("12345");
        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(true, valueMasker))
                .isEqualTo("true");
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

