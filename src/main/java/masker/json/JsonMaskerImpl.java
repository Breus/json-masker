package masker.json;

import org.jetbrains.annotations.NotNull;

interface JsonMaskerImpl {

    byte[] mask(byte[] input);

    @NotNull
    String mask(@NotNull String input);
}
