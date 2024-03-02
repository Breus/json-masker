package dev.blaauwendraad.masker.randomgen;

import dev.blaauwendraad.masker.json.util.JsonStringCharacters;

import java.math.BigInteger;
import java.util.Set;

public final class RandomJsonGeneratorConfig {
    private static final Set<String> defaultTargetKeys = Set.of("targetKey1", "targetKey2", "targetKey3", "targetKey4");

    private final int maxArraySize;
    private final float maxFloat;
    private final double maxDouble;
    private final long maxLong;
    private final BigInteger maxBigInt;
    private final int maxStringLength;
    private final int maxObjectKeys;
    private final int maxNodeDepth;
    private final double targetKeyPercentage; // percentage of object keys which are target keys
    private final Set<Character> allowedCharacters;
    private final Set<String> targetKeys;
    private final int targetJsonSizeBytes;
    private final long randomSeed;

    public RandomJsonGeneratorConfig(
            int maxArraySize,
            float maxFloat,
            double maxDouble,
            long maxLong,
            BigInteger maxBigInt,
            int maxStringLength,
            int maxObjectKeys,
            int maxNodeDepth,
            double targetKeyPercentage,
            Set<Character> allowedCharacters,
            Set<String> targetKeys,
            int targetJsonSizeBytes,
            long randomSeed
    ) {
        this.maxArraySize = maxArraySize;
        this.maxFloat = maxFloat;
        this.maxDouble = maxDouble;
        this.maxLong = maxLong;
        this.maxBigInt = maxBigInt;
        this.maxObjectKeys = maxObjectKeys;
        this.maxNodeDepth = maxNodeDepth;
        this.targetKeyPercentage = targetKeyPercentage;
        this.allowedCharacters = allowedCharacters;
        this.targetKeys = targetKeys;
        this.maxStringLength = maxStringLength;
        this.targetJsonSizeBytes = targetJsonSizeBytes;
        this.randomSeed = randomSeed;
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

    /**
     * @return max positive and negative float used by the RandomJsonGenerator
     */
    public float getMaxFloat() {
        return maxFloat;
    }

    /**
     * @return max positive and negative double used by the RandomJsonGenerator
     */
    public double getMaxDouble() {
        return maxDouble;
    }

    /**
     * @return max positive and negative long used by the RandomJsonGenerator
     */
    public long getMaxLong() {
        return maxLong;
    }

    /**
     * @return max positive and negative big integer used by the RandomJsonGenerator
     */
    public BigInteger getMaxBigInt() {
        return maxBigInt;
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

    public double getTargetKeyPercentage() {
        return targetKeyPercentage;
    }

    public Set<Character> getAllowedCharacters() {
        return allowedCharacters;
    }

    public int getTargetJsonSizeBytes() {
        return targetJsonSizeBytes;
    }

    public boolean hasTargetSize() {
        return targetJsonSizeBytes != -1;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public static class Builder {
        private int maxArraySize = 10;
        private float maxFloat = Float.MAX_VALUE;
        private double maxDouble = Double.MAX_VALUE;
        private long maxLong = Long.MAX_VALUE; // because we already have long, we don't add byte, short and int
        private BigInteger maxBigInt = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(Long.MAX_VALUE));
        private int maxStringLength = 8;
        private int maxObjectKeys = 5;
        private int maxNodeDepth = 10;
        private double targetKeyPercentage = 0.2;
        private Set<Character> allowedCharacters = JsonStringCharacters.mergeCharSets(
                JsonStringCharacters.getPrintableAsciiCharacters(),
                JsonStringCharacters.getUnicodeControlCharacters(),
                JsonStringCharacters.getRandomPrintableUnicodeCharacters()
        );
        private Set<String> targetKeys = defaultTargetKeys;
        private int targetJsonSizeBytes = -1; // no target, random size depending on other constraints
        private long randomSeed = 0; // no seed, using the ThreadLocalRandom()

        public Builder setMaxArraySize(int maxArraySize) {
            this.maxArraySize = maxArraySize;
            return this;
        }

        public Builder setMaxFloat(float maxFloat) {
            this.maxFloat = maxFloat;
            return this;
        }

        public void setMaxDouble(double maxDouble) {
            this.maxDouble = maxDouble;
        }

        public void setMaxLong(long maxLong) {
            this.maxLong = maxLong;
        }

        public void setMaxBigInt(BigInteger maxBigInt) {
            this.maxBigInt = maxBigInt;
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

        public Builder setTargetKeyPercentage(double targetKeyPercentage) {
            if (targetKeyPercentage < 0 || targetKeyPercentage > 1) {
                throw new IllegalArgumentException("targetKeyPercentage must be between 0 and 1");
            }
            this.targetKeyPercentage = targetKeyPercentage;
            return this;
        }

        public Builder setAllowedCharacters(Set<Character> allowedCharacters) {
            this.allowedCharacters = allowedCharacters;
            return this;
        }

        public Builder setTargetKeys(Set<String> targetKeys) {
            this.targetKeys = targetKeys;
            return this;
        }

        public Builder setTargetJsonSizeBytes(int targetJsonSizeBytes) {
            this.targetJsonSizeBytes = targetJsonSizeBytes;
            return this;
        }

        public Builder setRandomSeed(long randomSeed) {
            this.randomSeed = randomSeed;
            return this;
        }

        public RandomJsonGeneratorConfig createConfig() {
            return new RandomJsonGeneratorConfig(
                    maxArraySize,
                    maxFloat,
                    maxDouble,
                    maxLong,
                    maxBigInt,
                    maxStringLength,
                    maxObjectKeys,
                    maxNodeDepth,
                    targetKeyPercentage,
                    allowedCharacters,
                    targetKeys,
                    targetJsonSizeBytes,
                    randomSeed
            );
        }
    }
}