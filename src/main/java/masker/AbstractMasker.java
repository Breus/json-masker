package masker;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.util.Set;

public abstract class AbstractMasker implements MessageMasker {
    private final Set<String> targetKeys;

    protected AbstractMasker(@NotNull Set<String> targetKeys) {
        if (targetKeys.size() < 1) {
            throw new IllegalArgumentException("Target key set must contain at least on target key");
        }
        this.targetKeys = targetKeys;
    }

    @Override
    public abstract byte[] mask(byte[] message, @NotNull Charset charset);

    @Override
    @NotNull
    public abstract String mask(@NotNull String message);

    @NotNull
    public Set<String> getTargetKeys() {
        return targetKeys;
    }
}