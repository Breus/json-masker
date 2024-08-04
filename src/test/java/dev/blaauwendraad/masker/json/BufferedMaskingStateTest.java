package dev.blaauwendraad.masker.json;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

class BufferedMaskingStateTest {

    @Test
    void shouldReadInitialBuffer() {
        String json = "{" + "{\"maskMe\":\"val\"}".repeat(516) + "}";

        BufferedMaskingState maskingState = new BufferedMaskingState(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                new ByteArrayOutputStream(), false);

        Assertions.assertThat(maskingState.getMessage()).hasSize(8192);
    }

    @Test
    void shouldExtendCurrentBuffer() {
        String json = "{" + "{\"maskMe\":\"val\"}".repeat(516) + "}";
        BufferedMaskingState maskingState = new BufferedMaskingState(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                new ByteArrayOutputStream(), false);

        for (int i = 0; i < 700; i++) {
            maskingState.next();
        }

        maskingState.registerValueStartIndex();
        maskingState.readNextBuffer();

        Assertions.assertThat(maskingState.getMessage()).hasSize(7558);
    }

    @Test
    void shouldReadNextBuffer() {
        String json = "{" + "{\"maskMe\":\"val\"}".repeat(516) + "}";
        BufferedMaskingState maskingState = new BufferedMaskingState(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                new ByteArrayOutputStream(), false);

        for (int i = 0; i < 700; i++) {
            maskingState.next();
        }

        Assertions.assertThat(maskingState.getMessage()).hasSize(8192);
    }

}