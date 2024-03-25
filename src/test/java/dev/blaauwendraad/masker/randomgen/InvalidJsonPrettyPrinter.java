package dev.blaauwendraad.masker.randomgen;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static dev.blaauwendraad.masker.json.util.AsciiCharacter.CARRIAGE_RETURN;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.HORIZONTAL_TAB;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.LINE_FEED;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.SPACE;

/**
 * This class is DefaultPrettyPrinter with additional logic to add white spaces to JSON
 */
public class InvalidJsonPrettyPrinter implements PrettyPrinter {

    private static final List<Character> JSON_CONTROL_CHARACTERS = List.of(
            '{',
            '}',
            '[',
            ']',
            ':',
            '\'',
            '"',
            ',',
            'a',
            't',
            'f',
            'n',
            '-',
            '0',
            '1',
            '.',
            'e'
    );

    private final DefaultPrettyPrinter defaultPrettyPrinter = new DefaultPrettyPrinter();

    @Override
    public void writeRootValueSeparator(JsonGenerator gen) throws IOException {
        defaultPrettyPrinter.writeRootValueSeparator(gen);
    }

    @Override
    public void writeStartObject(JsonGenerator gen) throws IOException {
        addRandomJsonControlCharacter(gen);
        defaultPrettyPrinter.writeStartObject(gen);
        addRandomJsonControlCharacter(gen);
    }

    @Override
    public void writeEndObject(JsonGenerator gen, int nrOfEntries) throws IOException {
        addRandomJsonControlCharacter(gen);
        defaultPrettyPrinter.writeEndObject(gen, nrOfEntries);
        addRandomJsonControlCharacter(gen);
    }

    @Override
    public void writeObjectEntrySeparator(JsonGenerator gen) throws IOException {
        addRandomJsonControlCharacter(gen);
        defaultPrettyPrinter.writeObjectEntrySeparator(gen);
        addRandomJsonControlCharacter(gen);
    }

    @Override
    public void writeObjectFieldValueSeparator(JsonGenerator gen) throws IOException {
        addRandomJsonControlCharacter(gen);
        defaultPrettyPrinter.writeObjectFieldValueSeparator(gen);
        addRandomJsonControlCharacter(gen);
    }

    @Override
    public void writeStartArray(JsonGenerator gen) throws IOException {
        addRandomJsonControlCharacter(gen);
        defaultPrettyPrinter.writeStartArray(gen);
        addRandomJsonControlCharacter(gen);
    }

    @Override
    public void writeEndArray(JsonGenerator gen, int nrOfValues) throws IOException {
        addRandomJsonControlCharacter(gen);
        defaultPrettyPrinter.writeEndArray(gen, nrOfValues);
        addRandomJsonControlCharacter(gen);
    }

    @Override
    public void writeArrayValueSeparator(JsonGenerator gen) throws IOException {
        addRandomJsonControlCharacter(gen);
        defaultPrettyPrinter.writeArrayValueSeparator(gen);
        addRandomJsonControlCharacter(gen);
    }

    @Override
    public void beforeArrayValues(JsonGenerator gen) throws IOException {
        addRandomJsonControlCharacter(gen);
        defaultPrettyPrinter.beforeArrayValues(gen);
        addRandomJsonControlCharacter(gen);
    }

    @Override
    public void beforeObjectEntries(JsonGenerator gen) throws IOException {
        addRandomJsonControlCharacter(gen);
        defaultPrettyPrinter.beforeObjectEntries(gen);
        addRandomJsonControlCharacter(gen);
    }

    private void addRandomJsonControlCharacter(JsonGenerator gen) throws IOException {
        // insert invalid character with 10% chance
        if (ThreadLocalRandom.current().nextDouble(1) < 0.1) {
            int index = ThreadLocalRandom.current().nextInt(JSON_CONTROL_CHARACTERS.size());
            gen.writeRaw(JSON_CONTROL_CHARACTERS.get(index));
        }

    }
}
