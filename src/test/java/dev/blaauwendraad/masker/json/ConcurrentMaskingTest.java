package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class ConcurrentMaskingTest {
    private static final Duration MAX_CONCURRENT_TEST_RUN = Duration.ofSeconds(10);
    private static final String JSON_INPUT =
            """
        {
            "userInfo": {
                "personalInfo": {
                    "firstName": "Breus",
                    "lastName": "Blaauwendraad"
                },
                "ipAddress": "123.102.123",
                "userId": 1234
            }
        }""";

    private static final String EXPECTED_MASKED_OUTPUT =
            """
        {
            "userInfo": {
                "personalInfo": {
                    "firstName": "***",
                    "lastName": "***"
                },
                "ipAddress": "***",
                "userId": 1234
            }
        }""";

    @Test
    void concurrentJsonPathMasking() {
        JsonMasker masker =
                JsonMasker.getMasker(
                        JsonMaskingConfig.builder()
                                .maskJsonPaths("$.userInfo.personalInfo", "$.userInfo.ipAddress")
                                .build());
        maskConcurrently(Runtime.getRuntime().availableProcessors(), masker);
    }

    @Test
    void concurrentKeyMasking() {
        JsonMasker masker =
                JsonMasker.getMasker(
                        JsonMaskingConfig.builder().maskKeys("firstName", "lastName", "ipAddress").build());
        maskConcurrently(Runtime.getRuntime().availableProcessors(), masker);
    }

    private void maskConcurrently(int nThreads, JsonMasker masker) {
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        IntStream.range(0, nThreads)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    Instant testStartTime = Instant.now();
                    while (Instant.now().isBefore(testStartTime.plus(MAX_CONCURRENT_TEST_RUN))) {
                        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(masker, JSON_INPUT, EXPECTED_MASKED_OUTPUT);
                    }
                }, executorService))
                .toList()
                .forEach(CompletableFuture::join);
    }
}
