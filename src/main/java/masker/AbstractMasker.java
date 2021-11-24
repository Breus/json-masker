package masker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;

abstract class AbstractMasker implements MessageMasker {
    private String targetKey;
    private int targetKeyLength;
    private final MaskingConfig maskingConfiguration;

    AbstractMasker(@NotNull String targetKey, @Nullable MaskingConfig maskingConfiguration) {
        if (targetKey.length() < 1) {
            throw new IllegalArgumentException("Target key must contain at least one character");
        }
        this.targetKey = targetKey;
        this.targetKeyLength = targetKey.length();
        this.maskingConfiguration = maskingConfiguration;
    }

    @Override
    public abstract byte[] mask(byte[] message, @NotNull Charset charset);

    @Override
    @NotNull
    public abstract String mask(@NotNull String message);


    public void resetTargetKey(@NotNull String newTargetKey) {
        if (newTargetKey.length() < 1) {
            throw new IllegalArgumentException("Target key must contain at least one character");
        }
        this.targetKey = newTargetKey;
        this.targetKeyLength = newTargetKey.length();
    }

    String getTargetKey() {
        return targetKey;
    }

    int getTargetKeyLength() {
        return targetKeyLength;
    }

    MaskingConfig getMaskingConfiguration() {
        return maskingConfiguration;
    }
}
