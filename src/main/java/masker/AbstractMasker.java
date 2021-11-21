package masker;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

abstract class AbstractMasker implements MessageMasker {
    private final String targetKey;
    private final int targetKeyLength;

    @Override
    public abstract byte[] mask(byte[] message, @NotNull Charset charset);

    @Override
    @NotNull
    public abstract String mask(@NotNull String message);

    public AbstractMasker(String targetKey, int targetKeyLength) {
        this.targetKey = targetKey;
        this.targetKeyLength = targetKeyLength;
    }

    public String getTargetKey() {
        return targetKey;
    }

    public int getTargetKeyLength() {
        return targetKeyLength;
    }
}
