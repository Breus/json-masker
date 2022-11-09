package masker.json;

import masker.AsciiCharacter;

/**
 * Class containing util methods to set a particular target value length to a fixed size mask (used for length
 * obfuscation and ignoring escaping characters in String values).
 */
public final class FixedLengthTargetValueMaskUtil {
    private FixedLengthTargetValueMaskUtil() {
        // util
    }

    /**
     * Replaces a target value (byte slice) with a fixed length string consisting only of the mask bytes inside the
     * input bytes array.
     *
     * @param inputBytes          the input bytes array in which the target value is replaced with the fixed length
     *                            mask.
     * @param targetValueEndIndex the index of the last character of the target value in the input bytes array. This
     *                            would be the index of the closing quote for a string value.
     * @param fixedMaskLength     the length of the fixed-length mask byte string.
     * @param targetValueLength   the length of the target value slice.
     * @param maskByte            the byte used for each byte in the mask
     * @return a new array corresponding to the input bytes array with the target value replaced with a fixed length
     * mask
     */
    static byte[] replaceTargetValueWithFixedLengthMask(
            byte[] inputBytes,
            int targetValueEndIndex,
            int fixedMaskLength,
            int targetValueLength,
            byte maskByte
    ) {
        // Create new empty array with a length computed by the difference between requested fixed length and target
        // value length.
        byte[] newInputBytes = new byte[inputBytes.length + (fixedMaskLength - targetValueLength)];
        int targetValueStartIndex = targetValueEndIndex - targetValueLength;
        // Copy all bytes till the target value (including opening quotes of the string value to be replaced)
        System.arraycopy(inputBytes, 0, newInputBytes, 0, targetValueStartIndex);
        // Start from beginning of target value and loop amount of required masked characters
        for (int i = targetValueStartIndex; i < targetValueStartIndex + fixedMaskLength; i++) {
            newInputBytes[i] = maskByte; // add masking characters
        }
        // Append the rest of the original array starting from end of target value
        System.arraycopy(
                inputBytes,
                targetValueEndIndex,
                newInputBytes,
                targetValueStartIndex + fixedMaskLength,
                inputBytes.length - targetValueEndIndex
        );
        return newInputBytes;
    }

    static byte[] replaceTargetValueWithFixedLengthAsteriskMask(
            byte[] inputBytes,
            int closingQuoteIndex,
            int fixedLength,
            int targetValueLength
    ) {
        return replaceTargetValueWithFixedLengthMask(
                inputBytes,
                closingQuoteIndex,
                fixedLength,
                targetValueLength,
                AsciiCharacter.ASTERISK.getAsciiByteValue()
        );
    }
}
