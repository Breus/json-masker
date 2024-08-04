package dev.blaauwendraad.masker.json.util;

import dev.blaauwendraad.masker.json.JsonMasker;
import org.junit.jupiter.api.Assertions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class AssertionsUtil {

    /**
     * Asserts that JsonMasker result is the same when using bytes and streams API given the same input.
     *
     * @param jsonMasker an instance of JsonMasker
     * @param input the input JSON
     */
    public static void assertJsonMaskerApiEquivalence(JsonMasker jsonMasker, String input) {
        byte[] bytesOutput = jsonMasker.mask(input).getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream streamsOutput = new ByteArrayOutputStream();
        Assertions.assertDoesNotThrow(() -> jsonMasker.mask(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), streamsOutput));
        Assertions.assertEquals(new String(bytesOutput, StandardCharsets.UTF_8), streamsOutput.toString(StandardCharsets.UTF_8), input);
    }
}
