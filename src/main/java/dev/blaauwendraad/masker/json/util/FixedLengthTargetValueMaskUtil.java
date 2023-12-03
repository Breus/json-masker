package dev.blaauwendraad.masker.json.util;

import dev.blaauwendraad.masker.json.MaskingState;

/**
 * Class containing utility methods to set a particular target value length to a fixed size mask. This can be used for
 * length obfuscation and ignoring escaping characters in string values.
 */
public final class FixedLengthTargetValueMaskUtil {
    private FixedLengthTargetValueMaskUtil() {
        // util
    }

    /**
     * Replaces a target value (byte slice) with a fixed length string consisting only of the mask bytes inside the
     * input bytes array.
     *
     * @param fixedMaskLength   the length of the fixed-length mask byte string.
     * @param targetValueLength the length of the target value slice.
     * @param maskByte          the byte used for each byte in the mask
     * @return a new array corresponding to the input bytes array with the target value replaced with a fixed length
     * mask
     */
    public static void replaceTargetValueWithFixedLengthMask(
            MaskingState maskingState,
            int fixedMaskLength,
            int targetValueLength,
            byte maskByte
    ) {
        // Create new empty array with a length computed by the difference between requested fixed length and target
        // value length.
        byte[] newInputBytes = new byte[maskingState.getMessage().length + (fixedMaskLength - targetValueLength)];
        int targetValueStartIndex = maskingState.currentIndex() - targetValueLength;
        // Copy all bytes till the target value (including opening quotes of the string value to be replaced)
        System.arraycopy(maskingState.getMessage(), 0, newInputBytes, 0, targetValueStartIndex);
        // Start from beginning of target value and loop amount of required masked characters
        for (int i = targetValueStartIndex; i < targetValueStartIndex + fixedMaskLength; i++) {
            newInputBytes[i] = maskByte; // add masking characters
        }
        // Append the rest of the original array starting from end of target value
        System.arraycopy(
                maskingState.getMessage(),
                maskingState.currentIndex(),
                newInputBytes,
                targetValueStartIndex + fixedMaskLength,
                maskingState.getMessage().length - maskingState.currentIndex()
        );
        maskingState.setMessage(newInputBytes);
    }

    public static void replaceTargetValueWithFixedLengthAsteriskMask(
            MaskingState maskingState,
            int fixedLength,
            int targetValueLength
    ) {
        replaceTargetValueWithFixedLengthMask(
                maskingState,
                fixedLength,
                targetValueLength,
                AsciiCharacter.ASTERISK.getAsciiByteValue()
        );
    }
}
