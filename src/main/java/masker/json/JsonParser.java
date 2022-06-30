package masker.json;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class JsonParser {
    Reader reader;

    public JsonParser(byte[] jsonUtf8) {
        this.reader = new InputStreamReader(new ByteArrayInputStream(jsonUtf8), StandardCharsets.UTF_8);
    }
}
