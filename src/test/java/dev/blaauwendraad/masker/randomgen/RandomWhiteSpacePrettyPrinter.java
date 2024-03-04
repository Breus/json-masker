package dev.blaauwendraad.masker.randomgen;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import static dev.blaauwendraad.masker.json.util.AsciiCharacter.*;

/**
 * This class is DefaultPrettyPrinter with additional logic to add white spaces to JSON
 */
public class RandomWhiteSpacePrettyPrinter implements PrettyPrinter {
    private final DefaultPrettyPrinter defaultPrettyPrinter = new DefaultPrettyPrinter();

    private int numberOfWhiteSpacesToInject;

    public RandomWhiteSpacePrettyPrinter(int numberOfWhiteSpacesToInject) {
        this.numberOfWhiteSpacesToInject = numberOfWhiteSpacesToInject;
    }

    @Override
    public void writeRootValueSeparator(JsonGenerator gen) throws IOException {
        defaultPrettyPrinter.writeRootValueSeparator(gen);
    }

    @Override
    public void writeStartObject(JsonGenerator gen) throws IOException {
        defaultPrettyPrinter.writeStartObject(gen);
        addRandomWhiteSpace(gen);
    }

    @Override
    public void writeEndObject(JsonGenerator gen, int nrOfEntries) throws IOException {
        defaultPrettyPrinter.writeEndObject(gen, nrOfEntries);
        addRandomWhiteSpace(gen);
    }

    @Override
    public void writeObjectEntrySeparator(JsonGenerator gen) throws IOException {
        defaultPrettyPrinter.writeObjectEntrySeparator(gen);
        addRandomWhiteSpace(gen);
    }

    @Override
    public void writeObjectFieldValueSeparator(JsonGenerator gen) throws IOException {
        defaultPrettyPrinter.writeObjectFieldValueSeparator(gen);
        addRandomWhiteSpace(gen);
    }

    @Override
    public void writeStartArray(JsonGenerator gen) throws IOException {
        defaultPrettyPrinter.writeStartArray(gen);
        addRandomWhiteSpace(gen);
    }

    @Override
    public void writeEndArray(JsonGenerator gen, int nrOfValues) throws IOException {
        defaultPrettyPrinter.writeEndArray(gen, nrOfValues);
        addRandomWhiteSpace(gen);
    }

    @Override
    public void writeArrayValueSeparator(JsonGenerator gen) throws IOException {
        defaultPrettyPrinter.writeArrayValueSeparator(gen);
        addRandomWhiteSpace(gen);
    }

    @Override
    public void beforeArrayValues(JsonGenerator gen) throws IOException {
        defaultPrettyPrinter.beforeArrayValues(gen);
        addRandomWhiteSpace(gen);
    }

    @Override
    public void beforeObjectEntries(JsonGenerator gen) throws IOException {
        defaultPrettyPrinter.beforeObjectEntries(gen);
        addRandomWhiteSpace(gen);
    }

    private void addRandomWhiteSpace(JsonGenerator gen) throws IOException {
        if (numberOfWhiteSpacesToInject == 0) {
            return;
        }

        // we use nextInt(6) and randomInt < 3 to make there a 50% chance of setting white space
        int randomInt = ThreadLocalRandom.current().nextInt(6);
        if (randomInt < 3) {
            gen.writeRaw((char) getRandomWhiteSpaceByte(randomInt));
            numberOfWhiteSpacesToInject--;
        }
    }

    private byte getRandomWhiteSpaceByte(int randomInt) {
        return switch (randomInt) {
            case 0 -> HORIZONTAL_TAB.getAsciiByteValue();
            case 1 -> LINE_FEED.getAsciiByteValue();
            case 2 -> SPACE.getAsciiByteValue();
            default -> throw new IllegalStateException("Unexpected value to get random whitespace byte: " + randomInt);
        };
    }
}
