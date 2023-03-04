package masker.json;

import masker.AsciiCharacter;
import masker.Utf8Util;
import masker.json.config.JsonMaskingConfig;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static masker.AsciiCharacter.isDoubleQuote;
import static masker.AsciiCharacter.isEscapeCharacter;

public class SingleTargetMasker implements JsonMasker {
    private final Set<String> quotedTargetKeys;
    private final JsonMaskingConfig maskingConfig;

    public SingleTargetMasker(JsonMaskingConfig maskingConfig) {
        this.quotedTargetKeys = new HashSet<>();
        maskingConfig.getTargetKeys().forEach(t -> quotedTargetKeys.add('"' + t + '"'));
        this.maskingConfig = maskingConfig;
    }

    static int indexOf(byte[] src, byte[] target) {
        for (int i = 0; i <= src.length - target.length; i++) {
            boolean found = true;
            for (int j = 0; j < target.length; ++j) {
                if (src[i + j] != target[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public byte[] mask(byte[] message) {
        for (String targetKey : quotedTargetKeys) {
            message = mask(message, targetKey);
        }
        return message;
    }

    /**
     * Masks the String values in the given input for all values corresponding to the provided target key.
     * This implementation is optimized for a single target key.
     * Currently, only supports UTF_8/US_ASCII
     *
     * @param input     the UTF-8 encoded input bytes for which values of the target key are masked
     * @param targetKey the JSON key for which the String values are masked
     * @return the masked message
     */
    public byte[] mask(byte[] input, @NotNull String targetKey) {
        // With obfuscation enabled, this is used as output (so can eventually have different length than originalInput)
        byte[] outputBytes = input;
        int i = 0; // index based on current input slice
        int j = 0; // index based on input
        outer:
        // Smaller and minus two because when looking for a JSON key, the minimum value (0 length) would result in:
        // '{"key":2}'. So 1 for th colon, 1 for the value and 1 (smaller than) for the closing '}'.
        while (j < outputBytes.length - targetKey.getBytes().length - 2) {
            j = j + i;
            byte[] inputSliceBytes;
            if (j == 0) {
                inputSliceBytes = outputBytes;
            } else {
                inputSliceBytes = Arrays.copyOfRange(outputBytes, j, outputBytes.length);
            }
            int startIndexOfTargetKey = indexOf(inputSliceBytes, targetKey.getBytes(StandardCharsets.UTF_8));
            if (startIndexOfTargetKey == -1) {
                break; // Input doesn't contain target key anymore, no further masking required
            }
            i = startIndexOfTargetKey + targetKey.length(); // step over found target key
            for (; i < inputSliceBytes.length; i++) {
                if (AsciiJsonUtil.isWhiteSpace(inputSliceBytes[i])) {

                    continue; // Found a JSON whitespace, try next character
                }
                if (AsciiCharacter.isColon(inputSliceBytes[i])) {
                    break; // Found a colon, so the found target key is indeed a JSON key
                }
                // Found a different character than whitespace or colon, so the found target key is not a JSON key
                continue outer;
            }
            i++; // step over colon
            for (; i < inputSliceBytes.length; i++) {
                if (AsciiJsonUtil.isWhiteSpace(inputSliceBytes[i])) {
                    continue; // found a space, try next character
                }
                /*
                 * Whenever number masking is enabled, check if the next character could be the first character of a
                 * JSON number value.
                 */
                if (maskingConfig.isNumberMaskingEnabled() && AsciiJsonUtil.isFirstNumberChar(inputSliceBytes[i])) {
                    int targetValueLength = 0;
                    while (AsciiJsonUtil.isNumericCharacter(inputSliceBytes[i])) {
                        outputBytes[i + j] = AsciiCharacter.toAsciiByteValue(maskingConfig.getMaskNumberValuesWith());
                        targetValueLength++;
                        i++;
                    }
                    int obfuscationLength = maskingConfig.getObfuscationLength();
                    if (maskingConfig.isObfuscationEnabled() && obfuscationLength != targetValueLength) {
                        if (obfuscationLength == 0) {
                            /* For obfuscation length 0, we want to obfuscate numeric values with a single 0 because
                             * an empty numeric value is illegal JSON.
                             */
                            outputBytes = FixedLengthTargetValueMaskUtil.replaceTargetValueWithFixedLengthMask(
                                    outputBytes,
                                    i,
                                    1,
                                    targetValueLength,
                                    AsciiCharacter.toAsciiByteValue(maskingConfig.getMaskNumberValuesWith())
                            );
                            i = i - (targetValueLength - 1);
                        } else {
                            outputBytes = FixedLengthTargetValueMaskUtil.replaceTargetValueWithFixedLengthMask(
                                    outputBytes,
                                    i,
                                    obfuscationLength,
                                    targetValueLength,
                                    AsciiCharacter.toAsciiByteValue(maskingConfig.getMaskNumberValuesWith())
                            );
                            i = i - (targetValueLength - obfuscationLength);
                        }
                    }
                } else if (AsciiCharacter.isDoubleQuote(inputSliceBytes[i])) { // value is a string
                    i++; // step over quote
                    int targetValueLength = 0;
                    int noOfEscapeCharacters = 0;
                    int additionalBytesForEncoding = 0;
                    boolean escapeNextCharacter = false;
                    boolean previousCharacterCountedAsEscapeCharacter = false;
                    while (!isDoubleQuote(inputSliceBytes[i]) || (isDoubleQuote(inputSliceBytes[i])
                            && escapeNextCharacter)) {
                        if (Utf8Util.getCodePointByteLength(inputSliceBytes[i]) > 1) {
                            /*
                             *  UTF-8, so whenever code points are encoded using multiple bytes this should be
                             *  represented by a single asterisk and the additional bytes used for encoding need to be
                             *  removed.
                             */
                            additionalBytesForEncoding += Utf8Util.getCodePointByteLength(inputSliceBytes[i]) - 1;
                        }
                        escapeNextCharacter = isEscapeCharacter(inputSliceBytes[i]);
                        if (escapeNextCharacter && !previousCharacterCountedAsEscapeCharacter) {
                            /*
                             * Non-escaped backslashes are escape characters and are not actually part of the string
                             * but only used for encoding, so need not be included in the mask.
                             */
                            noOfEscapeCharacters++;
                            previousCharacterCountedAsEscapeCharacter = true;
                        } else {
                            if (previousCharacterCountedAsEscapeCharacter
                                    && AsciiCharacter.isLowercaseU(inputSliceBytes[i])) {
                                /*
                                 * Next 4 characters are hexadecimal digits which form a single character and are only
                                 * there for encoding unicode characters.
                                 */
                                additionalBytesForEncoding += 4;
                            }
                            previousCharacterCountedAsEscapeCharacter = false;
                        }
                        outputBytes[i + j] = AsciiCharacter.ASTERISK.getAsciiByteValue();
                        targetValueLength++;
                        i++;
                    }
                    int obfuscationLength = maskingConfig.getObfuscationLength();
                    if (maskingConfig.isObfuscationEnabled()
                            && obfuscationLength != targetValueLength - obfuscationLength) {
                        outputBytes = FixedLengthTargetValueMaskUtil.replaceTargetValueWithFixedLengthAsteriskMask(
                                outputBytes,
                                i + j,
                                obfuscationLength,
                                targetValueLength
                        ); // set reference of input bytes to the new array reference
                        i = i - (targetValueLength - obfuscationLength);
                    } else if (noOfEscapeCharacters > 0 || additionalBytesForEncoding > 0) {
                        // Remove escape characters and additional bytes required for unicode point character from
                        // the mask since they aren't actually part of the target value string.
                        int actualStringLength = targetValueLength - noOfEscapeCharacters - additionalBytesForEncoding;
                        outputBytes = FixedLengthTargetValueMaskUtil.replaceTargetValueWithFixedLengthAsteriskMask(
                                outputBytes,
                                i + j,
                                actualStringLength,
                                targetValueLength
                        );
                        i = i - noOfEscapeCharacters - additionalBytesForEncoding;
                    }
                }
                continue outer;
            }
        }
        return outputBytes;
    }
}