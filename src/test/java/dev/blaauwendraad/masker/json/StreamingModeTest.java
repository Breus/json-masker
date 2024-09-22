package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfigTestUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The test suite covers different circumstances under which the streaming buffer ends
 */
class StreamingModeTest {

    @ParameterizedTest
    @MethodSource("getBufferingSituations")
    void shouldHandleBufferingSuccessfully(Integer initialBufferSize,
                                           Set<String> maskKeys,
                                           String json,
                                           String expectedResult) {
        JsonMaskingConfig config = JsonMaskingConfig.builder().maskKeys(maskKeys).build();
        JsonMaskingConfigTestUtil.setBufferSize(JsonMaskingConfig.builder().maskKeys(maskKeys).build(), initialBufferSize);
        JsonMasker jsonMasker = JsonMasker.getMasker(config);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        jsonMasker.mask(inputStream, outputStream);

        Assertions.assertThat(outputStream).hasToString(expectedResult);
    }

    @Test
    void shouldAbortExecutionOnTooValueLongToken() {
        JsonMaskingConfig config = JsonMaskingConfig.builder().maskKeys("mask").build();
        JsonMasker jsonMasker = JsonMasker.getMasker(config);
        // maximum guaranteed JSON token size to work correctly is roughly 4 million characters, but can be up to
        // 16 million depending on where this token is located and the tokens before and after it
        String json = "{\"mask\":\"%s\"}".formatted("a".repeat(17_000_000));

        Assertions.assertThatThrownBy(() -> jsonMasker.mask(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                new ByteArrayOutputStream())
        ).isInstanceOf(InvalidJsonException.class);
    }

    @Test
    void shouldAbortExecutionOnTooLongKeyToken() {
        JsonMaskingConfig config = JsonMaskingConfig.builder().maskKeys("mask").build();
        JsonMasker jsonMasker = JsonMasker.getMasker(config);
        // maximum guaranteed JSON token size to work correctly is roughly 4 million characters, but can be up to
        // 16 million depending on where this token is located and the tokens before and after it
        String json = "{\"%s\":\"hello\"}".formatted("a".repeat(17_000_000));

        Assertions.assertThatThrownBy(() -> jsonMasker.mask(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                new ByteArrayOutputStream())
        ).isInstanceOf(InvalidJsonException.class);
    }

    private static Stream<Arguments> getBufferingSituations() {
        return Stream.of(
                // process json with a single buffer.
                Arguments.of(
                        1024,
                        Set.of("mask"),
                        "{\"mask\":\"value\"}",
                        "{\"mask\":\"***\"}"
                ),
                // process json with re-reading the buffer, no token start index is registered in between.
                // the buffer ends at the middle of not registered token ('0' in ': [0]')
                Arguments.of(
                        16,
                        Set.of("mask"),
                        "{\"doNotMask\": [0], \"mask\": \"a\"}",
                        "{\"doNotMask\": [0], \"mask\": \"***\"}"
                ),
                // process json with re-reading the buffer, no token start index is registered in between.
                // the buffer ends at comma (',' in '[0], ')
                Arguments.of(
                        18,
                        Set.of("mask"),
                        "{\"doNotMask\": [0], \"mask\": \"a\"}",
                        "{\"doNotMask\": [0], \"mask\": \"***\"}"
                ),
                // process json with re-reading the buffer, no token start index is registered in between.
                // the buffer ends at the opening of the array ('[' in ': [0]')
                Arguments.of(
                        17,
                        Set.of("mask"),
                        "{\"doNotMaskMe\": [0], \"mask\": \"a\"}",
                        "{\"doNotMaskMe\": [0], \"mask\": \"***\"}"
                ),
                // process json with re-reading the buffer, no token start index is registered in between.
                // the buffer ends at the opening of the nested object ('{' in ': {"doNot"')
                Arguments.of(
                        17,
                        Set.of("mask"),
                        "{\"doNotMaskMe\": {\"doNot\":\"ok\"}, \"mask\": \"a\"}",
                        "{\"doNotMaskMe\": {\"doNot\":\"ok\"}, \"mask\": \"***\"}"
                ),
                // process json with re-reading the buffer, no token start index is registered in between.
                // the buffer ends at the key/value separator (':' in ': [0]')
                Arguments.of(
                        13,
                        Set.of("mask"),
                        "{\"doNotMask\": [0], \"mask\": \"a\"}",
                        "{\"doNotMask\": [0], \"mask\": \"***\"}"
                ),
                // process json with re-reading the buffer, token start index is registered for a value.
                // the buffer ends in registered "value" value
                Arguments.of(
                        34,
                        Set.of("mask"),
                        "{\"doNotMask\": \"value\", \"mask\": \"value\"}",
                        "{\"doNotMask\": \"value\", \"mask\": \"***\"}"
                ),
                // process json with re-reading the buffer, token start index is registered for a key.
                // the buffer ends in registered "mask" key
                Arguments.of(
                        26,
                        Set.of("mask"),
                        "{\"doNotMask\": \"value\", \"mask\": \"value\"}",
                        "{\"doNotMask\": \"value\", \"mask\": \"***\"}"
                ),
                // process json with re-reading the buffer, token start index is registered for a value.
                // the buffer ends in registered "veryLongValue" value. The value length is equal to a threshold (quarter size of the buffer)
                Arguments.of(
                        12,
                        Set.of("mask"),
                        "{\"mask\": \"veryLongValue\", \"doNotMask\": \"value\"}",
                        "{\"mask\": \"***\", \"doNotMask\": \"value\"}"
                ),
                // process json with re-reading the buffer, token start index is registered for a value.
                // the buffer ends in registered "veryLongValue" value. The registered value length is larger than a threshold (quarter size of the buffer)
                Arguments.of(
                        12,
                        Set.of("mask"),
                        "{\"mask\":\"veryLongValue\", \"doNotMask\": \"value\"}",
                        "{\"mask\":\"***\", \"doNotMask\": \"value\"}"
                ),
                // process json with re-reading the buffer, token start index is registered for a value.
                // the buffer ends in a registered value whose length is far larger than the initial buffer size
                Arguments.of(
                        12,
                        Set.of("mask"),
                        "{\"mask\": \"veryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongValue\", \"doNotMask\": \"value\"}",
                        "{\"mask\": \"***\", \"doNotMask\": \"value\"}"
                ),
                // process json with re-reading the buffer, token start index is registered for a key.
                // the buffer ends in a registered key whose length is far larger than the initial buffer size
                Arguments.of(
                        12,
                        Set.of("maskThisVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongKey"),
                        "{\"doNotMask\": \"value\", \"maskThisVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongKey\": \"value\"}",
                        "{\"doNotMask\": \"value\", \"maskThisVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongKey\": \"***\"}"
                ),
                // process json with registered key and value far larger than the buffer size
                Arguments.of(
                        12,
                        Set.of("maskThisVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongKey"),
                        "{\"doNotMask\": \"value\", \"maskThisVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongKey\": \"veryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongValue\"}",
                        "{\"doNotMask\": \"value\", \"maskThisVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongKey\": \"***\"}"
                )
        );
    }
}
