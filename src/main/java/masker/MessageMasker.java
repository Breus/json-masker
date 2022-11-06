package masker;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

public interface MessageMasker {
    byte[] mask(byte[] message, @NotNull Charset charset);

    @NotNull String mask(@NotNull String message);
}
