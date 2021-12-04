package masker.json;

import masker.AbstractMasker;
import masker.UTF8Encoding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

final class JsonMasker extends AbstractMasker {
    private final JsonMaskingConfig maskingConfig;
    private Set<String> quotedTargetKeys; // TODO: only used for SINGLE_TARGET_LOOP algorithm, might want to remove somehow

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
        for (String targetKey : getQuotedTargetKeys()) {
            message = maskValuesOfTargetKey(new String(message, charset), targetKey).getBytes(charset);
        }
        return message;
    }

    @Override
    @NotNull
    public String mask(@NotNull String message) {
        if (getMaskingConfig().getMultiTargetAlgorithm() == JsonMultiTargetAlgorithm.SINGLE_TARGET_LOOP) {
            for (String targetKey : getQuotedTargetKeys()) {
                message = maskValuesOfTargetKey(message, targetKey);
            }
        } else if (getMaskingConfig().getMultiTargetAlgorithm() == JsonMultiTargetAlgorithm.KEYS_CONTAIN) {
            message = maskValueOfTargetKeys(message, getTargetKeys()); // TODO @robert: implement keys contain algorithm here
        }
        return message;
    }

    private JsonMasker(@NotNull Set<String> targetKeys, @Nullable JsonMaskingConfig maskingConfig) {
        super(targetKeys);
        if (maskingConfig == null) {
            this.maskingConfig = JsonMaskingConfig.getDefault();
        } else {
            this.maskingConfig = maskingConfig;
        }
        if (this.maskingConfig.getMultiTargetAlgorithm() == JsonMultiTargetAlgorithm.SINGLE_TARGET_LOOP) {
            // TODO @breus: initialize target key set with and without quotes depending on MultiTargetAlgorithm
            Set<String> quotedTargetKeys = new HashSet<>();
            targetKeys.forEach(t -> quotedTargetKeys.add('"' + t + '"'));
            this.quotedTargetKeys = quotedTargetKeys;
        }
    }


    /**
     * Masks the String values in the given input for all values corresponding to any of the provided target keys.
     * This implementation is optimized for multiple target keys.
     * Currently, only supports UTF_8/US_ASCII
     * @param input the input message for which values might be masked
     * @param targetKeys the set of JSON keys for which the String values are masked
     * @return the masked message
     */
    @NotNull
    String maskValueOfTargetKeys(@NotNull String input, @NotNull Set<String> targetKeys) {
        /*
            General approaches:
            1. Look for colon (:), and read back to find the key
                Pros: Read string is always a JSON key
                Drawbacks: Read all bytes of all JSON keys twice
                Worst case: high number of long JSON keys
            2. Look for " and check if it is a JSON key
                Pros: No double byte reading.
                Drawbacks: might spend way too much computation on string values in e.g. String arrays.
                Worst case: Lots of string values, especially when in an array.
            3. Look for opening ", but keep historic state to determine if current quote can be JSON key.
                Pros: no double byte reading, mitigate drawback of 2.
                Drawbacks: complexity
                Worst case: deeply nested JSON objects causing lots of state switching between canBeJsonKey

            We start implementing 1. because it's much less complex and its worst case is quite unlikely

            Pseudocode:
            1. Loop through input, look for JSON key (jsonkey)
            2. if (targetKeys.contains(jsonKey) // mask String value
            3. else continue

            1. Look for :, step back to read key
               1.1 Loop over all bytes, until byte equals UTF8Encoding.COLON
               1.2 Read back till closing DOUBLE_QUOTE
               1.3 Count characters from closing DOUBLE_QUOTE to opening DOUBLE_QUOTE
               1.4 Do a smart System.ArrayCopy to create a new String from the key (key)
               1.5 Check targetKeys.contains(key)

            3. Look for ", look ahead to check for ':'
                3.0 While last bracket is '[', continue while not ']' OR last bracket is '{'
                3.1 Loop over all bytes, until byte equal UTF8Encoding.DOUBLE_QUOTE
                3.2 Set startIndex
                3.3 Read till closing DOUBLE_QUOTE
                3.4 Set closeIndex
                3.5 If after spaces/tabs/enters is COLON? Then key.
                3.6 Substring of startIndex - closeIndex
                3.7 targetKeys.contains(key)

            3. Check if opening DOUBLE_QOUTE can be a JSON key:
               1. A JSON object has opened and no key has been found yet
               2. A JSON object has opened and a key has been found, but this has been reset with a comma
               3. [{"key": 12}, {"value": 12}]
         */
        // TODO @robert, @breus: implement this method according to method 1.

        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        int i = 0;
        while (i < inputBytes.length - 2) {
            if (inputBytes[i] != UTF8Encoding.COLON.getUtf8ByteValue())
                i++; // loop until index is on colon
            int colonIndex = i;
            while (inputBytes[i] != UTF8Encoding.DOUBLE_QUOTE.getUtf8ByteValue())
                i--; // loop back until index is on closing quote of key
            int closingQuoteIndex = i;
            while (inputBytes[i] != UTF8Encoding.DOUBLE_QUOTE.getUtf8ByteValue())
                i--; // loop back until index is on opening quote of key
            int openingQuoteIndex = i;
            int keyLength = closingQuoteIndex - openingQuoteIndex;
            byte[] keyBytes = new byte[keyLength];
            System.arraycopy(inputBytes, openingQuoteIndex, keyBytes, 0, keyLength);
            String key = new String(keyBytes, StandardCharsets.UTF_8);
            i = colonIndex + 1; // continue looping from after colon
            if (!targetKeys.contains(key)) {
                i = i + 5; // +5 since minimum amount of characters between colon is 5 --> {"a":1,"b":2}
                continue;
            }
            while (inputBytes[i] != UTF8Encoding.DOUBLE_QUOTE.getUtf8ByteValue())
                i++; // loop until index is on opening quote of value
            i++; // step over quote
            int targetValueLength = 0;
            while(inputBytes[i] != UTF8Encoding.DOUBLE_QUOTE.getUtf8ByteValue()) {
                inputBytes[i] = UTF8Encoding.ASTERISK.getUtf8ByteValue();
                targetValueLength++;
                i++;
            }
            int obfuscationLength = getMaskingConfiguration().getObfuscationLength();
            if (obfuscationLength != -1 && obfuscationLength != targetValueLength) {
                inputBytes = obfuscateLengthOfTargetValue(inputBytes, i, obfuscationLength, targetValueLength); // set reference of input bytes to the new array reference
            }
        }
        return new String(inputBytes, StandardCharsets.UTF_8);
    }

    /**
     * Masks the String values in the given input for all values corresponding to the provided target key.
     * This implementation is optimized for a single target key.
     * Currently, only supports UTF_8/US_ASCII
     * @param input the input message for which values might be masked
     * @param targetKey the JSON key for which the String values are masked
     * @return the masked message
     */
    @NotNull
    String  maskValuesOfTargetKey(@NotNull String input, @NotNull String targetKey) {
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        int i = 0; // index based on current input slice
        int j = 0; // index based on input
        outer: while (j < inputBytes.length - targetKey.length() - 2) { // minus 1 for closing bracket, smaller than because colon required for a new key and minus 1 for value with minimum length of 1
            j = j + i;
            String inputSlice;
            byte[] inputSliceBytes;
            if (j == 0) {
                inputSlice = input;
                inputSliceBytes = inputBytes;
            } else {
                inputSlice = input.substring(j);
                inputSliceBytes = inputSlice.getBytes(StandardCharsets.UTF_8);
            }
            int startIndexOfTargetKey = inputSlice.indexOf(targetKey);
            if(startIndexOfTargetKey == -1) {
                break; // input doesn't contain target key anymore, no further masking required
            }
            i = startIndexOfTargetKey + targetKey.length();
            for (; i < inputSliceBytes.length; i++) {
                if (inputSliceBytes[i] == UTF8Encoding.SPACE.getUtf8ByteValue()) {
                    continue; // found a space, try next character
                }
                if (inputSliceBytes[i] == UTF8Encoding.COLON.getUtf8ByteValue()) {
                    break; // found a colon, so the found target key is indeed a JSON key
                }
                continue outer; // found a different character than whitespace or colon, so the found target key is not a JSON key
            }
            i++; // step over colon
            for (; i < inputSliceBytes.length; i++) {
                if (inputSliceBytes[i] == UTF8Encoding.SPACE.getUtf8ByteValue()) {
                    continue; // found a space, try next character
                }
                if (inputSliceBytes[i] == UTF8Encoding.DOUBLE_QUOTE.getUtf8ByteValue()) { // value is a string
                    i++; // step over quote
                    int targetValueLength = 0;
                    while(inputSliceBytes[i] != UTF8Encoding.DOUBLE_QUOTE.getUtf8ByteValue()) {
                        inputBytes[i + j] = UTF8Encoding.ASTERISK.getUtf8ByteValue();
                        targetValueLength++;
                        i++;
                    }
                    int obfuscationLength = getMaskingConfig().getObfuscationLength();
                    if (obfuscationLength != -1 && obfuscationLength != targetValueLength) {
                        inputBytes = obfuscateLengthOfTargetValue(inputBytes, i, obfuscationLength, targetValueLength); // set reference of input bytes to the new array reference
                    }
                    continue outer;
                }
                continue outer;
            }
        }
        return new String(inputBytes, StandardCharsets.UTF_8);
    }

    byte[] obfuscateLengthOfTargetValue(byte[] inputBytes, int closingQuoteIndex, int obfuscationLength, int targetValueLength) {
        byte[] newInputBytes = new byte[inputBytes.length + (obfuscationLength - targetValueLength)]; // create new empty array with a length computed by the difference between obfuscation and target value length
        int targetValueStartIndex = closingQuoteIndex - targetValueLength;
        System.arraycopy(inputBytes, 0, newInputBytes, 0, targetValueStartIndex); // copy all bytes till the target value (including opening quotes)
        for (int i = targetValueStartIndex; i < targetValueStartIndex + obfuscationLength; i++) { // start from beginning of target value and loop amount of wanted masked characters
            newInputBytes[i] = UTF8Encoding.ASTERISK.getUtf8ByteValue(); // add masking characters
        }
        System.arraycopy(inputBytes, closingQuoteIndex, newInputBytes, targetValueStartIndex + obfuscationLength, inputBytes.length - closingQuoteIndex); // append rest of the original array starting from end of target value
        return newInputBytes;
    }

    public Set<String> getQuotedTargetKeys() {
        return quotedTargetKeys;
    }

    public JsonMaskingConfig getMaskingConfig() {
        return maskingConfig;
    }
}