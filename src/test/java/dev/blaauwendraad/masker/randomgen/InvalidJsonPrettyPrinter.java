package dev.blaauwendraad.masker.randomgen;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.util.DefaultPrettyPrinter;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
    public void writeRootValueSeparator(JsonGenerator gen) {
        defaultPrettyPrinter.writeRootValueSeparator(gen);
    }

    @Override
    public void writeStartObject(JsonGenerator gen) {
        addRandomJsonControlCharacter(gen);
        defaultPrettyPrinter.writeStartObject(gen);
        addRandomJsonControlCharacter(gen);
    }

    @Override
    public void writeEndObject(JsonGenerator gen, int nrOfEntries) {
        addRandomJsonControlCharacter(gen);
        defaultPrettyPrinter.writeEndObject(gen, nrOfEntries);
        addRandomJsonControlCharacter(gen);
    }

    @Override
    public void writeObjectEntrySeparator(JsonGenerator gen) {
        addRandomJsonControlCharacter(gen);
        defaultPrettyPrinter.writeObjectEntrySeparator(gen);
        addRandomJsonControlCharacter(gen);
    }

    @Override
    public void writeObjectNameValueSeparator(JsonGenerator gen) {
        addRandomJsonControlCharacter(gen);
        defaultPrettyPrinter.writeObjectEntrySeparator(gen);
        addRandomJsonControlCharacter(gen);
    }

    @Override
    public void writeStartArray(JsonGenerator gen) {
        addRandomJsonControlCharacter(gen);
        defaultPrettyPrinter.writeStartArray(gen);
        addRandomJsonControlCharacter(gen);
    }

    @Override
    public void writeEndArray(JsonGenerator gen, int nrOfValues) {
        addRandomJsonControlCharacter(gen);
        defaultPrettyPrinter.writeEndArray(gen, nrOfValues);
        addRandomJsonControlCharacter(gen);
    }

    @Override
    public void writeArrayValueSeparator(JsonGenerator gen) {
        addRandomJsonControlCharacter(gen);
        defaultPrettyPrinter.writeArrayValueSeparator(gen);
        addRandomJsonControlCharacter(gen);
    }

    @Override
    public void beforeArrayValues(JsonGenerator gen) {
        addRandomJsonControlCharacter(gen);
        defaultPrettyPrinter.beforeArrayValues(gen);
        addRandomJsonControlCharacter(gen);
    }

    @Override
    public void beforeObjectEntries(JsonGenerator gen) {
        addRandomJsonControlCharacter(gen);
        defaultPrettyPrinter.beforeObjectEntries(gen);
        addRandomJsonControlCharacter(gen);
    }

    private void addRandomJsonControlCharacter(JsonGenerator gen) {
        // insert invalid character with 10% chance
        if (ThreadLocalRandom.current().nextDouble(1) < 0.1) {
            int index = ThreadLocalRandom.current().nextInt(JSON_CONTROL_CHARACTERS.size());
            gen.writeRaw(JSON_CONTROL_CHARACTERS.get(index));
        }

    }
}
