package masker;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public abstract class AbstractMasker implements MessageMasker {
    private final Set<String> targetKeys;

    protected AbstractMasker(@NotNull Set<String> targetKeys) {
        if (targetKeys.isEmpty()) {
            throw new IllegalArgumentException("Target key set must contain at least on target key");
        }
        this.targetKeys = targetKeys;
    }

    @NotNull
    public Set<String> getTargetKeys() {
        return targetKeys;
    }
}