package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.util.ByteValueMaskerContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ValueMaskersTest {
    @Test
    void withStringValue() {
        var valueMasker = ValueMaskers.with("***");

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("\"secret\"", valueMasker))
                .isEqualTo("\"***\"");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("\"***\"");
        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(true, valueMasker))
                .isEqualTo("\"***\"");
    }

    @Test
    void withIntegerValue() {
        var valueMasker = ValueMaskers.with(0);

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("\"secret\"", valueMasker))
                .isEqualTo("0");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("0");
        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(true, valueMasker))
                .isEqualTo("0");
    }

    @Test
    void withBooleanValue() {
        var valueMasker = ValueMaskers.with(false);

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("\"secret\"", valueMasker))
                .isEqualTo("false");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("false");
        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(true, valueMasker))
                .isEqualTo("false");
    }

    @Test
    void withNull() {
        var valueMasker = ValueMaskers.withNull();

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("\"secret\"", valueMasker))
                .isEqualTo("null");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("null");
        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(true, valueMasker))
                .isEqualTo("null");
    }

    @Test
    void eachCharacterWith() {
        var valueMasker = ValueMaskers.eachCharacterWith("*");

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("\"secret\"", valueMasker))
                .isEqualTo("\"******\"");
    }

    @Test
    void eachDigitWith() {
        var valueMasker = ValueMaskers.eachDigitWith(1);

        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("11111");
    }

    @Test
    void noop() {
        var valueMasker = ValueMaskers.noop();

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("\"secret\"", valueMasker))
                .isEqualTo("\"secret\"");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("12345");
        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(true, valueMasker))
                .isEqualTo("true");
    }

    @Test
    void email() {
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "\"agavlyukovskiy@gmail.com\"",
                        ValueMaskers.email(2, 2, true, "***")
                ))
                .isEqualTo("\"ag***iy@gmail.com\"");
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "\"agavlyukovskiy@gmail.com\"",
                        ValueMaskers.email(0, 2, true, "***")
                ))
                .isEqualTo("\"***iy@gmail.com\"");
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "\"agavlyukovskiy@gmail.com\"",
                        ValueMaskers.email(0, 0, true, "***")
                ))
                .isEqualTo("\"***@gmail.com\"");
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "\"agavlyukovskiy@gmail.com\"",
                        ValueMaskers.email(0, 0, false, "***")
                ))
                .isEqualTo("\"***\"");
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "\"agavlyukovskiy@gmail.com\"",
                        ValueMaskers.email(2, 0, false, "***")
                ))
                .isEqualTo("\"ag***\"");
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "\"agavlyukovskiy@gmail.com\"",
                        ValueMaskers.email(0, 2, false, "***")
                ))
                .isEqualTo("\"***om\"");
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith(
                        "\"a@gmail.com\"",
                        ValueMaskers.email(2, 2, true, "***")
                ))
                .isEqualTo("\"a@gmail.com\"");
    }

    @Test
    void withTextFunction() {
        var valueMasker = ValueMaskers.withTextFunction(value -> {
            if (value.startsWith("secret:")) {
                return "***";
            }
            return value;
        });

        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("\"not a secret\"", valueMasker))
                .isEqualTo("\"not a secret\"");
        Assertions.assertThat(ByteValueMaskerContext.maskStringWith("\"secret: very much\"", valueMasker))
                .isEqualTo("\"***\"");
        Assertions.assertThat(ByteValueMaskerContext.maskNumberWith(12345, valueMasker))
                .isEqualTo("\"12345\"");
        Assertions.assertThat(ByteValueMaskerContext.maskBooleanWith(true, valueMasker))
                .isEqualTo("\"true\"");
    }
}