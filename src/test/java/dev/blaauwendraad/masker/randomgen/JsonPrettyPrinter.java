package dev.blaauwendraad.masker.randomgen;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JsonPrettyPrinter implements PrettyPrinter {
    private int indent = 0;
    private boolean isNewline = true;
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private final byte[] bytes;
    public JsonPrettyPrinter(byte[] bytes){
        this.bytes = bytes;
    }

    public byte[] getWhiteSpaceInjectedJson() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setDefaultPrettyPrinter(this);
        String result = null;
        try {
            JsonNode node = mapper.readTree(this.bytes);
            result = mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(node);
        }
        catch (IOException e){
            System.out.println(e.getCause().getMessage());
        }
        assert result != null;
        return result.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void writeStartObject(JsonGenerator jg) throws IOException {
        if (!isNewline)
            newline(jg);
        jg.writeRaw('{');
        ++ indent;
        isNewline = false;
    }

    @Override
    public void beforeObjectEntries(JsonGenerator jg) throws IOException {
        newline(jg);
    }

    @Override
    public void writeObjectFieldValueSeparator(JsonGenerator jg) throws IOException {
        jg.writeRaw(" : ");
        isNewline = false;
    }

    @Override
    public void writeObjectEntrySeparator(JsonGenerator jg) throws IOException {
        jg.writeRaw(",");
        newline(jg);
    }

    @Override
    public void writeEndObject(JsonGenerator jg, int nrOfEntries) throws IOException {
        -- indent;
        newline(jg);
        jg.writeRaw('}');
        isNewline = indent == 0;
    }

    @Override
    public void writeStartArray(JsonGenerator jg) throws IOException {
        newline(jg);
        jg.writeRaw("[");
        ++ indent;
        isNewline = false;
    }

    @Override
    public void beforeArrayValues(JsonGenerator jg) throws IOException {
        newline(jg);
    }

    @Override
    public void writeArrayValueSeparator(JsonGenerator jg) throws IOException {
        jg.writeRaw(", ");
        isNewline = false;
    }

    @Override
    public void writeEndArray(JsonGenerator jg, int nrOfValues) throws IOException {
        -- indent;
        newline(jg);
        jg.writeRaw(']');
        isNewline = false;
    }

    @Override
    public void writeRootValueSeparator(JsonGenerator jg) throws IOException {
        jg.writeRaw(' ');
    }

    private void newline(JsonGenerator jg) throws IOException {
        jg.writeRaw(LINE_SEPARATOR);
        for (int i = 0; i < indent; ++ i) {
            jg.writeRaw("  ");
        }
        isNewline = true;
    }
}
