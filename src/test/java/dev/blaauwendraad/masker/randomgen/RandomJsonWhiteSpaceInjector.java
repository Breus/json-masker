package dev.blaauwendraad.masker.randomgen;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class RandomJsonWhiteSpaceInjector {
    private final byte[] originalJsonBytes;
    private final int maxNumberOfWhiteSpacesToInject;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final JsonGenerator jsonGenerator;

    public RandomJsonWhiteSpaceInjector(byte[] originalJsonBytes, int maxNumberOfWhiteSpacesToInject) throws IOException {
        this.originalJsonBytes = originalJsonBytes;
        this.maxNumberOfWhiteSpacesToInject = maxNumberOfWhiteSpacesToInject;

        jsonGenerator = new JsonFactory().createGenerator(outputStream);
    }

    public byte[] getWhiteSpaceInjectedJson() {
        try {
            RandomWhiteSpacePrettyPrinter prettyPrinter = new RandomWhiteSpacePrettyPrinter(maxNumberOfWhiteSpacesToInject);
            outputStream.reset();
            Object json = objectMapper.readValue(originalJsonBytes, Object.class);
            objectMapper.writer(prettyPrinter).writeValue(jsonGenerator, json);
            jsonGenerator.close();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
