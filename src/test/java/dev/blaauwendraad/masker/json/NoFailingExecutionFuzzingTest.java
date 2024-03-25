package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.databind.JsonNode;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.util.FuzzingDurationUtil;
import dev.blaauwendraad.masker.json.util.JsonFormatter;
import dev.blaauwendraad.masker.randomgen.RandomJsonGenerator;
import dev.blaauwendraad.masker.randomgen.RandomJsonGeneratorConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.fail;

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
        long timeLimit = FuzzingDurationUtil.determineTestTimeLimit(failureFuzzingConfigurations().count());
        System.out.printf(
                "Running tests with the following JSON masking configuration: \n%s",
                jsonMaskingConfig);
        Instant startTime = Instant.now();
        AtomicInteger randomTestsExecuted = new AtomicInteger();
        AtomicReference<String> lastExecutedJson = new AtomicReference<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<Void> backgroundTest =
                CompletableFuture.runAsync(
                        () -> {
                            while (Duration.between(startTime, Instant.now()).toMillis() < timeLimit) {
                                generateAndMask(jsonMaskingConfig, lastExecutedJson, randomTestsExecuted);
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

    private static void generateAndMask(JsonMaskingConfig jsonMaskingConfig, AtomicReference<String> lastExecutedJson, AtomicInteger randomTestsExecuted) {
        KeyContainsMasker keyContainsMasker = new KeyContainsMasker(jsonMaskingConfig);
        RandomJsonGenerator randomJsonGenerator =
                new RandomJsonGenerator(RandomJsonGeneratorConfig.builder().createConfig());
        JsonNode jsonNode = randomJsonGenerator.createRandomJsonNode();
        for (JsonFormatter formatter : JsonFormatter.values()) {
            String mutatedJsonNodeString = formatter.format(jsonNode);
            lastExecutedJson.set(mutatedJsonNodeString);
            if (formatter.isValid()) {
                Assertions.assertDoesNotThrow(
                        () -> keyContainsMasker.mask(mutatedJsonNodeString),
                        String.format(
                                "Failed for the following JSON:\n%s",
                                mutatedJsonNodeString));
            } else {
                try {
                    keyContainsMasker.mask(mutatedJsonNodeString);
                } catch (RuntimeException e) {
                    // ignoring failures for an invalid json
                }
            }
            randomTestsExecuted.incrementAndGet();
        }
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
