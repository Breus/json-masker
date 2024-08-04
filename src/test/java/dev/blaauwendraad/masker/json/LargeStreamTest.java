package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
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

    static final String INPUT_FILE_NAME = "input.json";
    static final String EXPECTED_RESULT_FILE_NAME = "expected_result.json";
    static final String ACTUAL_RESULT_FILE_NAME = "actual_result.json";

    @BeforeAll
    static void setupFiles() throws IOException {
        // prepare input.json file and expected_result.json
        FileWriter inputFileWriter = new FileWriter(INPUT_FILE_NAME);
        FileWriter expectedResultFileWriter = new FileWriter(EXPECTED_RESULT_FILE_NAME);
        String line = "{\"key\":\"mask\"},".repeat(1024);
        String expectedResultLine = "{\"key\":\"***\"},".repeat(1024);
        inputFileWriter.write("[");
        expectedResultFileWriter.write("[");
        for (int i = 0; i < 65536; i++) {
            inputFileWriter.write(line);
            expectedResultFileWriter.write(expectedResultLine);
            inputFileWriter.flush();
            expectedResultFileWriter.flush();
        }
        inputFileWriter.write("{\"key\":\"mask\"}");
        expectedResultFileWriter.write("{\"key\":\"***\"}");
        inputFileWriter.flush();
        expectedResultFileWriter.flush();
        inputFileWriter.write("]");
        expectedResultFileWriter.write("]");
        inputFileWriter.close();
        expectedResultFileWriter.close();
    }

    @Test
    @Disabled("Run it manually")
    void shouldProcessLargeJson() throws IOException {
        // process the input file and write the result into actual_result.json file
        FileInputStream inputStream = new FileInputStream(INPUT_FILE_NAME);
        FileOutputStream outputStream = new FileOutputStream(ACTUAL_RESULT_FILE_NAME);
        JsonMasker jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder().allowKeys().build());
        jsonMasker.mask(inputStream, outputStream);
        inputStream.close();
        outputStream.close();

        // assert correctness of the result
        FileInputStream expectedResultFile = new FileInputStream(EXPECTED_RESULT_FILE_NAME);
        FileInputStream actualResultFile = new FileInputStream(ACTUAL_RESULT_FILE_NAME);
        String expected = new String(expectedResultFile.readNBytes(512), StandardCharsets.UTF_8);
        String actual = new String(actualResultFile.readNBytes(512), StandardCharsets.UTF_8);
        while (!expected.isEmpty() || !actual.isEmpty()) {
            Assertions.assertEquals(expected, actual);
            expected = new String(expectedResultFile.readNBytes(512), StandardCharsets.UTF_8);
            actual = new String(actualResultFile.readNBytes(512), StandardCharsets.UTF_8);
        }
        Assertions.assertEquals(0, expectedResultFile.available());
        Assertions.assertEquals(0, actualResultFile.available());
        expectedResultFile.close();
        actualResultFile.close();
    }

    @AfterAll
    static void cleanUpFiles() throws IOException {
        // clean up files
        Files.deleteIfExists(Path.of(INPUT_FILE_NAME));
        Files.deleteIfExists(Path.of(EXPECTED_RESULT_FILE_NAME));
        Files.deleteIfExists(Path.of(ACTUAL_RESULT_FILE_NAME));
    }
}
