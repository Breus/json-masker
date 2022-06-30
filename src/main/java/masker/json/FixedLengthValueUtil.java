package masker.json;

import masker.Utf8AsciiCharacter;

/**
 * Class containing util methods to set a particular target value length to a fixed size (used for length obfuscation and ignoring escaping characters in String values).
 */
public final class FixedLengthValueUtil {
    private FixedLengthValueUtil() {
        // util
    }

    // TargetValueEndIndex = the index of the closing quote for a string value
    static byte[] setFixedLengthOfValue(byte[] inputBytes, int targetValueEndIndex, int fixedLength, int targetValueLength, byte replacementCharacter) {
        byte[] newInputBytes = new byte[inputBytes.length + (fixedLength - targetValueLength)]; // create new empty array with a length computed by the difference between requested fixed length and target value length
        int targetValueStartIndex = targetValueEndIndex - targetValueLength;
        System.arraycopy(inputBytes, 0, newInputBytes, 0, targetValueStartIndex); // copy all bytes till the target value (including opening quotes)
        for (int i = targetValueStartIndex; i < targetValueStartIndex + fixedLength; i++) { // start from beginning of target value and loop amount of wanted masked characters
            newInputBytes[i] = replacementCharacter; // add masking characters
        }
        System.arraycopy(inputBytes, targetValueEndIndex, newInputBytes, targetValueStartIndex + fixedLength, inputBytes.length - targetValueEndIndex); // append rest of the original array starting from end of target value
        return newInputBytes;
    }

    static byte[] setFixedLengthOfStringValue(byte[] inputBytes, int closingQuoteIndex, int fixedLength, int targetValueLength) {
        return setFixedLengthOfValue(inputBytes, closingQuoteIndex, fixedLength, targetValueLength, Utf8AsciiCharacter.ASTERISK.getUtf8ByteValue());
    }
}
