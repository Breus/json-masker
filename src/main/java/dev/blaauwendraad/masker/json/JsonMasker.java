package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Masker that can be used to mask JSON objects and arrays.
 */
public interface JsonMasker {

    /**
     * Creates a default {@link JsonMasker} with the provided target key(s).
     *
     * @param targetKeys the key(s) to target
     * @return the {@link JsonMasker} instance
     */
    static JsonMasker getMasker(Set<String> targetKeys) {
        return getMasker(JsonMaskingConfig.builder().maskKeys(targetKeys).build());
    }

    /**
     * Creates a {@link JsonMasker} with the provided {@link JsonMaskingConfig}.
     *
     * @param maskingConfig the JSON masker configuration
     * @return a new {@link JsonMasker} instance corresponding to the provided {@link JsonMaskingConfig}
     */
    static JsonMasker getMasker(JsonMaskingConfig maskingConfig) {
        return new KeyContainsMasker(maskingConfig);
    }

    /**
     * Masks the given JSON input and returns the masked output.
     *
     * @param input the JSON input as bytes
     * @return the masked JSON output as bytes
     * @throws InvalidJsonException in case invalid JSON input was provided
     */
    byte[] mask(byte[] input);

    /**
     * Masks the given JSON input stream and writes the result into the output stream.
     *
     * @param inputStream  the JSON input stream
     * @param outputStream masked JSON output stream
     * @throws InvalidJsonException in case invalid JSON input was provided
     * @throws UncheckedIOException if an I/O error occurs while reading from the input stream or writing to the output
     *                              stream
     */
    void mask(InputStream inputStream, OutputStream outputStream);

    /**
     * Masks the given JSON input and returns the masked output.
     *
     * @param input the JSON input as String
     * @return the masked JSON output String
     * @throws InvalidJsonException in case invalid JSON input was provided
     */
    default String mask(String input) {
        return new String(mask(input.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }
}
