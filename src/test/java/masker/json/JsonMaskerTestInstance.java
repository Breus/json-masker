package masker.json;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.*;

public final class JsonMaskerTestInstance {
    private final Set<String> targetKeys;
    private final String input;
    private final String expectedOutput;
    private Map<String, Object> maskerConfigs;

    public enum MaskerConfigKey {
        obfuscationLength,
        maskNumberValuesWith;
    }

    public JsonMaskerTestInstance(Set<String> targetKeys, String input,
                                  String expectedOutput, Map<String, Object> maskingConfigs) {
        this.targetKeys = targetKeys;
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.maskerConfigs = maskingConfigs;
    }

    public Set<String> targetKeys() {
        return targetKeys;
    }

    public String input() {
        return input;
    }

    public String expectedOutput() {
        return expectedOutput;
    }

    public void addMaskerConfig(MaskerConfigKey maskerConfigKey, String configValue) {
        if (maskerConfigs == null) {
            maskerConfigs = new HashMap<>();
        }
        maskerConfigs.put(maskerConfigKey.name(), configValue);
    }

    public int obfuscationLength() {
        return (int) maskerConfigs.getOrDefault(MaskerConfigKey.obfuscationLength.name(), -1);
    }

    public int maskNumbersWithValue() {
        return (int) maskerConfigs.getOrDefault(MaskerConfigKey.maskNumberValuesWith.name(), -1);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (JsonMaskerTestInstance) obj;
        return Objects.equals(this.targetKeys, that.targetKeys) &&
                Objects.equals(this.input, that.input) &&
                Objects.equals(this.expectedOutput, that.expectedOutput) &&
                Objects.equals(this.maskerConfigs, that.maskerConfigs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetKeys, input, expectedOutput, maskerConfigs);
    }

    @Override
    public String toString() {
        return "JsonMaskerTestInstance[" +
                "targetKeys=" + targetKeys + ", " +
                "input=" + input + ", " +
                "expectedOutput=" + expectedOutput + ", " +
                "maskerConfigs=" + maskerConfigs + ']';
    }

}