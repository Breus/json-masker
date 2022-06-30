package masker.json;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

interface JsonMaskerImpl {

    byte[] mask(byte[] input);

    @NotNull
    default String mask(@NotNull String input) {
        return new String(mask(input.getBytes(StandardCharsets.UTF_8)),StandardCharsets.UTF_8);
    }
}
