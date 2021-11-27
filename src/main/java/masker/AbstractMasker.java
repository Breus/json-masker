package masker;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.util.Set;

abstract class AbstractMasker implements MessageMasker {
    private final MaskingConfig maskingConfiguration;
    private final Set<String> targetKeys;

    AbstractMasker(@NotNull Set<String> targetKeys, @NotNull MaskingConfig maskingConfiguration) {
        if (targetKeys.size() < 1) {
            throw new IllegalArgumentException("Target key set must contain at least on target key");
        }
        this.targetKeys = targetKeys;
        this.maskingConfiguration = maskingConfiguration;
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

    @NotNull
    MaskingConfig getMaskingConfiguration() {
        return maskingConfiguration;
    }
}
