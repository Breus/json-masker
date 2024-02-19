package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.databind.JsonNode;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.util.AsciiCharacter;
import dev.blaauwendraad.masker.json.util.AsciiJsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import randomgen.json.RandomJsonGenerator;
import randomgen.json.RandomJsonGeneratorConfig;
import randomgen.json.RandomJsonWhiteSpaceInjector;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import static dev.blaauwendraad.masker.json.util.AsciiCharacter.*;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.SPACE;
import static org.assertj.core.api.Assertions.fail;

/**
 * This class contains fuzzing tests which are meant to spot infinite loops and program failures for
 * all combination of {@link JsonMasker} and {@link JsonMaskingConfig}.
 *
 * <p>For each {@link JsonMaskingConfig}, random JSON inputs are generated against which the masker
 * runs and the only thing that is tested it doesn't cause an exception or gets stuck in a loop.
 */
final class NoFailingExecutionFuzzingTest {
    private static final Duration DEFAULT_TEST_INSTANCE_DURATION = Duration.ofSeconds(4);
    // This timeout also includes mutating the JSON (e.g. injecting random whitespaces)
    private static final Duration JSON_MASKING_TIMEOUT = Duration.ofSeconds(1);

    @ParameterizedTest
    @MethodSource("failureFuzzingConfigurations")
    // duration in seconds the tests runs for
    void regularRandomJson(JsonMaskingConfig jsonMaskingConfig, Duration durationToRunEachTest) {
        executeTest(jsonMaskingConfig, durationToRunEachTest, null);
    }

    /**
     * This test is a fuzzing test that randomly injects some number of {@link
     * AsciiJsonUtil#isWhiteSpace(byte)} characters in randomly generated JSON in such a way that
     * the result will also be valid JSON and then the whitespace injected JSON is masked.
     */
    @ParameterizedTest
    @MethodSource("failureFuzzingConfigurations")
    void whitespaceInjectedRandomJson(JsonMaskingConfig jsonMaskingConfig, Duration durationToRunEachTest) {
        executeTest(
                jsonMaskingConfig,
                durationToRunEachTest,
                (bytes) -> new RandomJsonWhiteSpaceInjector(bytes, 50).getWhiteSpaceInjectedJson());
    }

    private void executeTest(
            JsonMaskingConfig jsonMaskingConfig, Duration durationToRunEachTest, Function<byte[], byte[]> jsonMutator) {
        String jsonMutatorUsageMessage =
                jsonMutator != null ? "WITH a JSON mutator function" : "WITHOUT a JSON mutator function";
        System.out.printf(
                "Running tests with the following JSON masking configuration: \n%s and %s",
                jsonMaskingConfig, jsonMutatorUsageMessage);
        Instant startTime = Instant.now();
        AtomicInteger randomTestsExecuted = new AtomicInteger();
        AtomicReference<String> lastExecutedJson = new AtomicReference<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<Void> backgroundTest =
                CompletableFuture.runAsync(
                        () -> {
                            while (Instant.ofEpochMilli(System.currentTimeMillis())
                                    .isBefore(startTime.plus(durationToRunEachTest))) {
                                KeyContainsMasker keyContainsMasker = new KeyContainsMasker(jsonMaskingConfig);
                                RandomJsonGenerator randomJsonGenerator =
                                        new RandomJsonGenerator(RandomJsonGeneratorConfig.builder().createConfig());
                                byte[] jsonBytes =
                                        randomJsonGenerator.createRandomJsonString().getBytes(StandardCharsets.UTF_8);
                                byte[] mutatedJsonBytes;
                                if (jsonMutator != null) {
                                    mutatedJsonBytes = jsonMutator.apply(jsonBytes);
                                } else {
                                    mutatedJsonBytes = jsonBytes;
                                }
                                lastExecutedJson.set(new String(mutatedJsonBytes, StandardCharsets.UTF_8));
                                Assertions.assertDoesNotThrow(
                                        () -> keyContainsMasker.mask(mutatedJsonBytes),
                                        String.format(
                                                "Failed for the following JSON:\n%s",
                                                new String(mutatedJsonBytes, StandardCharsets.UTF_8)));
                                randomTestsExecuted.incrementAndGet();
                            }
                        },
                        executor);
        try {
            int lastCheckedNumberOfTests = 0;
            while (Instant.ofEpochMilli(System.currentTimeMillis()).isBefore(startTime.plus(durationToRunEachTest))) {
                Thread.sleep(JSON_MASKING_TIMEOUT.toMillis());
                if (backgroundTest.isCompletedExceptionally()) {
                    // test got completed exceptionally before the timeout, fail fast here as we're not supposed to get
                    // any exceptions
                    // joining to rethrow the underlying exception
                    backgroundTest.join();
                }
                int currentNumberOfExecutedTests = randomTestsExecuted.get();
                if (currentNumberOfExecutedTests == lastCheckedNumberOfTests) {
                    fail(
                            String.format(
                                    "The test got stuck after executing %d test when the following JSON was being processed: \n %s",
                                    randomTestsExecuted.get(), lastExecutedJson));
                }
                lastCheckedNumberOfTests = currentNumberOfExecutedTests;
            }
        } catch (InterruptedException e) {
            fail(
                    String.format(
                            "The test was interrupted after executing %d test when the following JSON was being processed: \n %s",
                            randomTestsExecuted.get(), lastExecutedJson));
        } finally {
            executor.shutdownNow();
        }
        System.out.printf(
                "Successfully executed %d randomly generated test scenarios in %d seconds. ",
                randomTestsExecuted.get(), durationToRunEachTest.toSeconds());
    }

    @Nonnull
    private static Stream<Arguments> failureFuzzingConfigurations() {
        Set<String> targetKeys = Set.of("targetKey1", "targetKey2");
        return Stream.of(
                // Mask mode
                Arguments.of(JsonMaskingConfig.builder().maskKeys(targetKeys).build(), DEFAULT_TEST_INSTANCE_DURATION),
                Arguments.of(
                        JsonMaskingConfig.builder().maskKeys(targetKeys).caseSensitiveTargetKeys().build(),
                        DEFAULT_TEST_INSTANCE_DURATION),
                Arguments.of(
                        JsonMaskingConfig.builder()
                                .maskKeys(targetKeys)
                                .maskStringCharactersWith("*")
                                .maskNumberDigitsWith(1)
                                .build(),
                        DEFAULT_TEST_INSTANCE_DURATION),
                Arguments.of(
                        JsonMaskingConfig.builder()
                                .maskKeys(targetKeys)
                                .maskStringsWith("*")
                                .maskNumbersWith(1)
                                .build(),
                        DEFAULT_TEST_INSTANCE_DURATION),
                Arguments.of(
                        JsonMaskingConfig.builder()
                                .maskKeys(targetKeys)
                                .maskStringsWith("**")
                                .maskNumbersWith(11)
                                .build(),
                        DEFAULT_TEST_INSTANCE_DURATION),
                // Allow mode
                Arguments.of(JsonMaskingConfig.builder().allowKeys(targetKeys).build(), DEFAULT_TEST_INSTANCE_DURATION),
                Arguments.of(
                        JsonMaskingConfig.builder().allowKeys(targetKeys).caseSensitiveTargetKeys().build(),
                        DEFAULT_TEST_INSTANCE_DURATION),
                Arguments.of(
                        JsonMaskingConfig.builder().allowKeys(targetKeys).maskNumberDigitsWith(1).build(),
                        DEFAULT_TEST_INSTANCE_DURATION),
                Arguments.of(
                        JsonMaskingConfig.builder()
                                .allowKeys(targetKeys)
                                .maskStringsWith("*")
                                .maskNumbersWith(1)
                                .build(),
                        DEFAULT_TEST_INSTANCE_DURATION),
                Arguments.of(
                        JsonMaskingConfig.builder()
                                .allowKeys(targetKeys)
                                .maskStringsWith("**")
                                .maskNumbersWith(11)
                                .build(),
                        DEFAULT_TEST_INSTANCE_DURATION));
    }
}
