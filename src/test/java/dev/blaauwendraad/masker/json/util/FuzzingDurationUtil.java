package dev.blaauwendraad.masker.json.util;

import java.time.Duration;

public class FuzzingDurationUtil {
    public static final Duration TOTAL_TEST_DURATION_SECONDS = Duration.ofSeconds(isCi() ? 300 : 30);

    private static boolean isCi() {
        return System.getenv("CI") != null;
    }

    /**
     * Determines time limit for an individual test so that whole tests suite executes in either 30 seconds (local)
     * or 5 minutes (CI)
     */
    public static long determineTestTimeLimit(long numberOfTests) {
        return TOTAL_TEST_DURATION_SECONDS.toMillis() / numberOfTests;
    }
}
