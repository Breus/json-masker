package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests that the masker is able to process streams of any size in buffering mode.
 * The test creates ~1GB input file and the expected result file. Then it runs the masker in buffering mode and checks
 * that the actual result matches the expected result.
 * Keeping this test disabled because I am not sure if running it on CI is a good idea
 */
final class LargeStreamTest {
    Path inputFilePath;
    Path expectedResultFilePath;
    Path actualResultFilePath;

    @BeforeEach
    void setupFiles(@TempDir Path tempDir) throws IOException {
        inputFilePath = tempDir.resolve("input.json");
        expectedResultFilePath = tempDir.resolve("expected_result.json");
        actualResultFilePath = tempDir.resolve("actual_result.json");
        for (int i = 0; i < 65536; i++) {
            Files.writeString(inputFilePath, "{\"key\":\"mask\"},".repeat(1024));
            Files.writeString(expectedResultFilePath, "{\"key\":\"***\"},".repeat(1024));
        }
        Files.writeString(inputFilePath, "{\"key\":\"mask\"}");
        Files.writeString(expectedResultFilePath, "{\"key\":\"***\"}");
    }

    @Test
    @Disabled("Run it manually")
    void shouldProcessLargeJson() throws IOException {
        // process the input file and write the result into actual_result.json file
        try (InputStream inputStream = Files.newInputStream(inputFilePath);
             OutputStream outputStream = Files.newOutputStream(actualResultFilePath)) {
            JsonMasker jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder().allowKeys().build());
            jsonMasker.mask(inputStream, outputStream);
        }

        // assert correctness of the result
        try (InputStream expectedResultFile = Files.newInputStream(expectedResultFilePath);
             InputStream actualResultFile = Files.newInputStream(actualResultFilePath)) {
            String expected = new String(expectedResultFile.readNBytes(512), StandardCharsets.UTF_8);
            String actual = new String(actualResultFile.readNBytes(512), StandardCharsets.UTF_8);
            while (!expected.isEmpty() || !actual.isEmpty()) {
                Assertions.assertEquals(expected, actual);
                expected = new String(expectedResultFile.readNBytes(512), StandardCharsets.UTF_8);
                actual = new String(actualResultFile.readNBytes(512), StandardCharsets.UTF_8);
            }
            Assertions.assertEquals(0, expectedResultFile.available());
            Assertions.assertEquals(0, actualResultFile.available());
        }
    }

}
