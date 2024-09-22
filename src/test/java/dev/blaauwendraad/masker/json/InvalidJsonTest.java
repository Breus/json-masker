package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class InvalidJsonTest {

    private final JsonMasker jsonMasker = JsonMasker.getMasker(
            JsonMaskingConfig.builder()
                    .allowKeys("allowMe")
                    .build()
    );

    @Test
    void arrayWithInvalidCharacterAfterValue() {
        maskBytesWithinTimeLimit("[\"value\"a]");
        maskStreamsWithinTimeLimit("[\"value\"a]");
    }

    @Test
    void objectWithInvalidCharacterAfterKey() {
        maskBytesWithinTimeLimit("{\"key\":\"value\"a}}}}");
        maskStreamsWithinTimeLimit("{\"key\":\"value\"a}}}}");
    }

    @Test
    void notFinishedString() {
        maskBytesWithinTimeLimit("\"a");
        maskStreamsWithinTimeLimit("\"a");
    }

    private void maskBytesWithinTimeLimit(String json) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> jsonMasker.mask(json));
        try {
            future.get(50, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            // execution failed with an exception, that's acceptable
            Assertions.assertThat(e.getCause())
                    .isInstanceOfAny(InvalidJsonException.class);
        } catch (InterruptedException | TimeoutException e) {
            Assertions.fail("Masking of %s hasn't completed within 50ms.".formatted(json));
        } finally {
            future.cancel(true);
            executor.shutdownNow();
        }
    }

    private void maskStreamsWithinTimeLimit(String json) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> jsonMasker.mask(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                new ByteArrayOutputStream()
        ));
        try {
            future.get(50, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            // execution failed with an exception, that's acceptable
            Assertions.assertThat(e.getCause())
                    .isInstanceOfAny(InvalidJsonException.class);
        } catch (InterruptedException | TimeoutException e) {
            Assertions.fail("Masking of %s hasn't completed within 50ms.".formatted(json));
        } finally {
            future.cancel(true);
            executor.shutdownNow();
        }
    }
}
