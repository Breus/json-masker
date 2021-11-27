package masker;

import java.util.Set;

public class JsonMaskerTestInstance {
    private Set<String> targetKeys;
    private String input;
    private String expectedOutput;
    private Integer obfuscationLength;

    public JsonMaskerTestInstance(Set<String> targetKeys, String input, String expectedOutput, Integer obfuscationLength) {
        this.targetKeys = targetKeys;
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.obfuscationLength = obfuscationLength;
    }

    public Set<String> getTargetKeys() {
        return targetKeys;
    }

    public String getInput() {
        return input;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public Integer getObfuscationLength() {
        return obfuscationLength;
    }

    @Override
    public String toString() {
        return "JsonMaskerTestInstance{" +
                "targetKeys=" + targetKeys +
                ", input='" + input + '\'' +
                ", expectedOutput='" + expectedOutput + '\'' +
                ", obfuscationLength=" + obfuscationLength +
                '}';
    }
}
