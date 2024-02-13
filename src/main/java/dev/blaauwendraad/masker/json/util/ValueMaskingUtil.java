package dev.blaauwendraad.masker.json.util;

import dev.blaauwendraad.masker.json.MaskingState;

/**
 * Class containing utility methods to replace a particular target value with a mask. This can be used for
 * length obfuscation and ignoring escaping characters in string values.
 */
public final class ValueMaskingUtil {
    private ValueMaskingUtil() {
        // util
    }

    /**
     * Replaces a target value (byte slice) with a mask byte. If lengths of both target value and mask are equal, the
     * replacement is done in-place, otherwise a replacement operation is recorded to be performed as a batch using
     * {@link #flushReplacementOperations}.
     *
     * @param maskingState      the masking state
     * @param targetValueLength the length of the target value slice.
     * @param replacementBytes  the bytes to replace the value slice with.
     */
    public static void replaceTargetValueWith(
            MaskingState maskingState,
            int targetValueLength,
            byte[] replacementBytes
    ) {
        // TODO: we use this method to replace the value with a fixed string including quotes (if any)
        //  that would add more allocations, for the values we could actually cache, but that's for later
        int targetValueStartIndex = maskingState.currentIndex() - targetValueLength;
        maskingState.addReplacementOperation(targetValueStartIndex, maskingState.currentIndex(), replacementBytes);
    }

    /**
     * Performs all replacement operations to the message array, must be called at the end of the replacements.
     * <p>
     * For every operation that required resizing of the original array, to avoid copying the array multiple times,
     * those operations were stored in a list and can be performed in one go, thus resizing the array only once.
     * <p>
     * Replacement operation is only recorded if the length of the target value is different from the length of the mask,
     * otherwise the replacement must have been done in-place.
     *
     * @param maskingState the current state of the {@link dev.blaauwendraad.masker.json.JsonMasker} instance.
     */
    public static void flushReplacementOperations(MaskingState maskingState) {
        if (maskingState.getReplacementOperations().isEmpty()) {
            return;
        }

        // Create new empty array with a length computed by the difference of all mismatches of lengths between the target values and the masks
        // in some edge cases the length difference might be equal to 0, but since some indices mismatch (otherwise there would be no replacement operations)
        // we still have to copy the array to keep track of data according to original indices
        byte[] newMessage = new byte[maskingState.getMessage().length + maskingState.getReplacementOperationsTotalDifference()];

        // Index of the original message array
        int index = 0;
        // Offset is the difference between the original and new array indices, we need it to calculate indices
        // in the new message array using startIndex and endIndex, which are indices in the original array
        int offset = 0;
        for (MaskingState.ReplacementOperation replacementOperation : maskingState.getReplacementOperations()) {
            // Copy everything from message up until replacement operation start index
            System.arraycopy(
                    maskingState.getMessage(),
                    index,
                    newMessage,
                    index + offset,
                    replacementOperation.startIndex() - index
            );
            // Insert the mask bytes
            System.arraycopy(
                    replacementOperation.maskBytes(),
                    0,
                    newMessage,
                    replacementOperation.startIndex() + offset,
                    replacementOperation.maskBytes().length
            );
            // Adjust index and offset to continue copying from the end of the replacement operation
            index = replacementOperation.endIndex();
            offset += replacementOperation.difference();
        }

        // Copy the remainder of the original array
        System.arraycopy(
                maskingState.getMessage(),
                index,
                newMessage,
                index + offset,
                maskingState.getMessage().length - index
        );

        maskingState.setMessage(newMessage);
        maskingState.setCurrentIndex(newMessage.length);
    }
}
