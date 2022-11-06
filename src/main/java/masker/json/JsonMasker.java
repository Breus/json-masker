package masker.json;

import masker.AbstractMasker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

final class JsonMasker extends AbstractMasker {
    private final JsonMaskerAlgorithm maskerImpl;

    @NotNull
    public static JsonMasker getMasker(@NotNull String targetKey) {
        return getMasker(targetKey, null);
    }

    @NotNull
    public static JsonMasker getMasker(@NotNull String targetKey, @Nullable JsonMaskingConfig maskingConfig) {
        return getMasker(Set.of(targetKey), maskingConfig);
    }

    @NotNull
    public static JsonMasker getMasker(@NotNull Set<String> targetKeys) {
        return getMasker(targetKeys, null);
    }

    @NotNull
    public static JsonMasker getMasker(@NotNull Set<String> targetKeys, @Nullable JsonMaskingConfig maskingConfig) {
        return new JsonMasker(targetKeys, maskingConfig);
    }

    @Override
    public byte[] mask(byte[] message, @NotNull Charset charset) {
        if (!StandardCharsets.UTF_8.equals(charset)) {
            throw new IllegalArgumentException("Json maskers only support UTF-8 charset");
        }
        return maskerImpl.mask(message);
    }

    @NotNull
    @Override
    public String mask(@NotNull String message) {
        return maskerImpl.mask(message);
    }

    private JsonMasker(@NotNull Set<String> targetKeys, @Nullable JsonMaskingConfig jsonMaskingConfig) {
        super(targetKeys);
        JsonMaskingConfig maskingConfig = (jsonMaskingConfig != null ? jsonMaskingConfig : JsonMaskingConfig.getDefault());
        if (maskingConfig.getMultiTargetAlgorithm() == JsonMultiTargetAlgorithm.SINGLE_TARGET_LOOP) {
            this.maskerImpl = new SingleTargetMasker(targetKeys, maskingConfig);
        } else {
            this.maskerImpl = new KeyContainsMasker(targetKeys, maskingConfig);
        }
    }
}