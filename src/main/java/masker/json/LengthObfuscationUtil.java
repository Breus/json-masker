package masker.json;

import masker.Utf8AsciiCharacter;

public final class LengthObfuscationUtil {
    private LengthObfuscationUtil() {
        // util
    }

    // TargetValueEndIndex = the index of the closing quote for a string value
    static byte[] obfuscationLengthOfValue(byte[] inputBytes, int targetValueEndIndex, int obfuscationLength, int targetValueLength, byte replacementCharacter) {
        byte[] newInputBytes = new byte[inputBytes.length + (obfuscationLength - targetValueLength)]; // create new empty array with a length computed by the difference between obfuscation and target value length
        int targetValueStartIndex = targetValueEndIndex - targetValueLength;
        System.arraycopy(inputBytes, 0, newInputBytes, 0, targetValueStartIndex); // copy all bytes till the target value (including opening quotes)
        for (int i = targetValueStartIndex; i < targetValueStartIndex + obfuscationLength; i++) { // start from beginning of target value and loop amount of wanted masked characters
            newInputBytes[i] = replacementCharacter; // add masking characters
        }
        System.arraycopy(inputBytes, targetValueEndIndex, newInputBytes, targetValueStartIndex + obfuscationLength, inputBytes.length - targetValueEndIndex); // append rest of the original array starting from end of target value
        return newInputBytes;
    }

    static byte[] obfuscateLengthOfStringValue(byte[] inputBytes, int closingQuoteIndex, int obfuscationLength, int targetValueLength) {
        return obfuscationLengthOfValue(inputBytes, closingQuoteIndex, obfuscationLength, targetValueLength, Utf8AsciiCharacter.ASTERISK.getUtf8ByteValue());
    }
}
