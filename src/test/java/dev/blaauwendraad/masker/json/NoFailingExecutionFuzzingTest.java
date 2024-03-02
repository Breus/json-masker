package dev.blaauwendraad.masker.json;

import static org.assertj.core.api.Assertions.fail;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.util.AsciiJsonUtil;

import dev.blaauwendraad.masker.json.util.FuzzingDurationUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.blaauwendraad.masker.randomgen.RandomJsonGenerator;
import dev.blaauwendraad.masker.randomgen.RandomJsonGeneratorConfig;
import dev.blaauwendraad.masker.randomgen.RandomJsonWhiteSpaceInjector;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * This class contains fuzzing tests which are meant to spot infinite loops and program failures for
 * all combination of {@link JsonMasker} and {@link JsonMaskingConfig}.
 *
 * <p>For each {@link JsonMaskingConfig}, random JSON inputs are generated against which the masker
 * runs and the only thing that is tested it doesn't cause an exception or gets stuck in a loop.
 */
final class NoFailingExecutionFuzzingTest {
    // This timeout also includes mutating the JSON (e.g. injecting random whitespaces)
    private static final Duration JSON_MASKING_TIMEOUT = Duration.ofSeconds(1);

    @ParameterizedTest
    @MethodSource("failureFuzzingConfigurations")
    // duration in seconds the tests runs for
    void regularRandomJson(JsonMaskingConfig jsonMaskingConfig) {
        executeTest(jsonMaskingConfig, null);
    }

    /**
     * This test is a fuzzing test that randomly injects some number of {@link
     * AsciiJsonUtil#isWhiteSpace(byte)} characters in randomly generated JSON in such a way that
     * the result will also be valid JSON and then the whitespace injected JSON is masked.
     */
    @ParameterizedTest
    @MethodSource("failureFuzzingConfigurations")
    void whitespaceInjectedRandomJson(JsonMaskingConfig jsonMaskingConfig) {
        executeTest(
                jsonMaskingConfig,
                (bytes) -> new RandomJsonWhiteSpaceInjector(bytes, 50).getWhiteSpaceInjectedJson());
    }

    private void executeTest(JsonMaskingConfig jsonMaskingConfig, @CheckForNull Function<byte[], byte[]> jsonMutator) {
        long timeLimit = FuzzingDurationUtil.determineTestTimeLimit(failureFuzzingConfigurations().count());
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
                            while (Duration.between(startTime, Instant.now()).toMillis() < timeLimit) {
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
            while (Duration.between(startTime, Instant.now()).toMillis() < timeLimit) {
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
                "Successfully executed %d randomly generated test scenarios in %dms. ",
                randomTestsExecuted.get(), timeLimit);
    }

    @Nonnull
    private static Stream<Arguments> failureFuzzingConfigurations() {
        Set<String> targetKeys = Set.of("targetKey1", "targetKey2");
        return Stream.of(
                // Mask mode
                Arguments.of(JsonMaskingConfig.builder().maskKeys(targetKeys).build()),
                Arguments.of(JsonMaskingConfig.builder().maskKeys(targetKeys).caseSensitiveTargetKeys().build()),
                Arguments.of(
                        JsonMaskingConfig.builder()
                                .maskKeys(targetKeys)
                                .maskStringCharactersWith("*")
                                .maskNumberDigitsWith(1)
                                .build()
                ),
                Arguments.of(
                        JsonMaskingConfig.builder()
                                .maskKeys(targetKeys)
                                .maskStringsWith("*")
                                .maskNumbersWith(1)
                                .build()
                ),
                Arguments.of(
                        JsonMaskingConfig.builder()
                                .maskKeys(targetKeys)
                                .maskStringsWith("**")
                                .maskNumbersWith(11)
                                .build()
                ),
                // Allow mode
                Arguments.of(JsonMaskingConfig.builder().allowKeys(targetKeys).build()),
                Arguments.of(JsonMaskingConfig.builder().allowKeys(targetKeys).caseSensitiveTargetKeys().build()),
                Arguments.of(JsonMaskingConfig.builder().allowKeys(targetKeys).maskNumberDigitsWith(1).build()),
                Arguments.of(
                        JsonMaskingConfig.builder()
                                .allowKeys(targetKeys)
                                .maskStringsWith("*")
                                .maskNumbersWith(1)
                                .build()
                ),
                Arguments.of(
                        JsonMaskingConfig.builder()
                                .allowKeys(targetKeys)
                                .maskStringsWith("**")
                                .maskNumbersWith(11)
                                .build()
                        ));
    }
}
