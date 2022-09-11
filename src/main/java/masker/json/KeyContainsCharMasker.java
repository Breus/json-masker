package masker.json;

import org.jetbrains.annotations.NotNull;

public class KeyContainsCharMasker implements JsonMaskerImpl {

    @Override
    public byte[] mask(byte[] input) {
        return new byte[0];
    }

    @Override
    public @NotNull String mask(@NotNull String input) {
        return JsonMaskerImpl.super.mask(input);
    }
}
