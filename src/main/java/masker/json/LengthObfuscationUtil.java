package masker.json;

import masker.UTF8Encoding;

public final class LengthObfuscationUtil {
    private LengthObfuscationUtil() {
        // util
    }

    static byte[] obfuscateLengthOfTargetValue(byte[] inputBytes, int closingQuoteIndex, int obfuscationLength, int targetValueLength) {
        byte[] newInputBytes = new byte[inputBytes.length + (obfuscationLength - targetValueLength)]; // create new empty array with a length computed by the difference between obfuscation and target value length
        int targetValueStartIndex = closingQuoteIndex - targetValueLength;
        System.arraycopy(inputBytes, 0, newInputBytes, 0, targetValueStartIndex); // copy all bytes till the target value (including opening quotes)
        for (int i = targetValueStartIndex; i < targetValueStartIndex + obfuscationLength; i++) { // start from beginning of target value and loop amount of wanted masked characters
            newInputBytes[i] = UTF8Encoding.ASTERISK.getUtf8ByteValue(); // add masking characters
        }
        System.arraycopy(inputBytes, closingQuoteIndex, newInputBytes, targetValueStartIndex + obfuscationLength, inputBytes.length - closingQuoteIndex); // append rest of the original array starting from end of target value
        return newInputBytes;
    }
}
