package randomgen.json;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RandomJsonGeneratorConfig {
    private static final Set<Character> asciiDigits = IntStream.rangeClosed(48, 57).mapToObj(i -> (char) i).collect(Collectors.toSet());
    private static final Set<Character> asciiUppercaseLetters = IntStream.rangeClosed(65, 90).mapToObj(i -> (char) i).collect(Collectors.toSet());
    private static final Set<Character> asciiLowercaseLetters = IntStream.rangeClosed(97, 122).mapToObj(i -> (char) i).collect(Collectors.toSet());
    private static final Set<String> defaultTargetKeys = Set.of("targetKey1", "targetKey2", "targetKey3");

    private final int maxArraySize;
    private final float maxFloat;
    private final int maxStringLength;
    private final int maxObjectKeys;
    private final int maxNodeDepth;
    private final int targetKeyPercentage; // percentage of object keys which are target keys
    private final Set<Character> stringCharacters;
    private final Set<String> targetKeys;

    public RandomJsonGeneratorConfig(int maxArraySize, float maxFloat, int maxStringLength, int maxObjectKeys, int maxNodeDepth, int targetKeyPercentage, Set<Character> stringCharacters, Set<String> targetKeys) {
        this.maxArraySize = maxArraySize;
        this.maxFloat = maxFloat;
        this.maxObjectKeys = maxObjectKeys;
        this.maxNodeDepth = maxNodeDepth;
        this.targetKeyPercentage = targetKeyPercentage;
        this.stringCharacters = stringCharacters;
        this.targetKeys = targetKeys;
        this.maxStringLength = maxStringLength;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getMaxArraySize() {
        return maxArraySize;
    }

    public int getMaxStringLength() {
        return maxStringLength;
    }

    public float getMaxFloat() {
        return maxFloat;
    }

    public int getMaxObjectKeys() {
        return maxObjectKeys;
    }

    public int getMaxNodeDepth() {
        return maxNodeDepth;
    }

    public Set<String> getTargetKeys() {
        return targetKeys;
    }

    public int getTargetKeyPercentage() {
        return targetKeyPercentage;
    }

    public Set<Character> getStringCharacters() {
        return stringCharacters;
    }

    public static class Builder {
        private int maxArraySize = 4;
        private float maxFloat = Float.MAX_VALUE;
        private int maxStringLength = 4;
        private int maxObjectKeys = 4;
        private int maxNodeDepth = 4;
        private int targetKeyPercentage = 20;
        private Set<Character> stringCharacters = mergeCharSets(asciiDigits, asciiLowercaseLetters, asciiUppercaseLetters);
        private Set<String> targetKeys = defaultTargetKeys;

        public Builder setMaxArraySize(int maxArraySize) {
            this.maxArraySize = maxArraySize;
            return this;
        }

        public Builder setMaxFloat(float maxFloat) {
            this.maxFloat = maxFloat;
            return this;
        }

        public Builder setMaxStringLength(int maxStringLength) {
            this.maxStringLength = maxStringLength;
            return this;
        }

        public Builder setMaxObjectKeys(int maxObjectKeys) {
            this.maxObjectKeys = maxObjectKeys;
            return this;
        }

        public Builder setMaxNodeDepth(int maxNodeDepth) {
            this.maxNodeDepth = maxNodeDepth;
            return this;
        }

        public Builder setTargetKeyPercentage(int targetKeyPercentage) {
            this.targetKeyPercentage = targetKeyPercentage;
            return this;
        }

        public Builder setStringCharacters(Set<Character> stringCharacters) {
            this.stringCharacters = stringCharacters;
            return this;
        }

        public Builder setTargetKeys(Set<String> targetKeys) {
            this.targetKeys = targetKeys;
            return this;
        }

        public RandomJsonGeneratorConfig createConfig() {
            return new RandomJsonGeneratorConfig(maxArraySize, maxFloat, maxStringLength, maxObjectKeys, maxNodeDepth, targetKeyPercentage, stringCharacters, targetKeys);
        }

        @SafeVarargs
        static Set<Character> mergeCharSets(Set<Character>... charSet){
            Set<Character> mergedSet = new HashSet<>();
            for (Set<Character> characters : charSet) {
                mergedSet.addAll(characters);
            }
            return mergedSet;
        }
    }
}