package dev.blaauwendraad.masker.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ByteArrayJsonScannerTest {

    @Test
    void shouldFindQuoteAndBackslashAtEveryAlignment() {
        for (int length = 1; length <= 32; length++) {
            for (int targetIndex = 0; targetIndex < length; targetIndex++) {
                byte[] bytes = new byte[length];
                Arrays.fill(bytes, (byte) 'a');

                bytes[targetIndex] = '"';
                assertThat(ByteArrayJsonScanner.indexOfQuoteOrBackslash(bytes, 0, bytes.length))
                        .isEqualTo(targetIndex);

                bytes[targetIndex] = '\\';
                assertThat(ByteArrayJsonScanner.indexOfQuoteOrBackslash(bytes, 0, bytes.length))
                        .isEqualTo(targetIndex);
            }
        }
    }

    @Test
    void shouldOnlyInspectRequestedRange() {
        byte[] bytes = new byte[20];
        Arrays.fill(bytes, (byte) 'a');
        bytes[0] = '\\';
        bytes[18] = '\\';
        bytes[19] = '"';

        assertThat(ByteArrayJsonScanner.indexOfQuoteOrBackslash(bytes, 1, 18)).isEqualTo(18);
        assertThat(ByteArrayJsonScanner.indexOfQuoteOrBackslash(bytes, 1, bytes.length - 1))
                .isEqualTo(18);
        assertThat(ByteArrayJsonScanner.indexOfQuoteOrBackslash(bytes, 19, bytes.length))
                .isEqualTo(19);
    }

    @Test
    void shouldReturnRangeEndWhenNoCandidateExists() {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) 'a');

        assertThat(ByteArrayJsonScanner.indexOfQuoteOrBackslash(bytes, 3, 29)).isEqualTo(29);
    }
}
