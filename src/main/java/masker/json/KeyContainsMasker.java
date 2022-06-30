package masker.json;

import masker.Utf8AsciiCharacter;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static masker.Utf8AsciiCharacter.*;
import static masker.json.Utf8AsciiJson.*;

public final class KeyContainsMasker implements JsonMaskerImpl {
    private final Set<String> targetKeys;
    private final JsonMaskingConfig maskingConfig;

    public KeyContainsMasker(Set<String> targetKeys, JsonMaskingConfig maskingConfig) {
        this.targetKeys = targetKeys;
        this.maskingConfig = maskingConfig;
    }


    /**
     * Masks the values in the given input for all values having keys corresponding to any of the provided target keys.
     * This implementation is optimized for multiple target keys.
     * Currently, only supports UTF_8/US_ASCII
     * @param input the input message for which values might be masked
     * @return the masked message
     */
    @Override
    public byte[] mask(byte[] input) {
        if (isStartOfValue(input[0])) {
            // The input is a JSON value which cannot contain any keys so no masking is required
            return input;
        }
        int i = 0;
        outer:
        while (i < input.length) {
            // minus one character for closing curly bracket, one for the value
            // skip characters until index is on a colon (to find a JSON key) which is not part of a String (key or value)
            if (! isColon(input[i])) {
                // Following line cannot result in ArrayOutOfBound because of the early return after checking for first char being a double quote
                i++;
                continue;
            }
            int colonIndex = i;
            i++; // step over the colon

            // first check that the value for this key is a string to see if we need to do masking
            while (isWhiteSpace(input[i])) {
                i++; // step over all white characters after the colon
            }
            if (! isDoubleQuote(input[i])) {
                if (maskingConfig.isNumberMaskingDisabled()) {
                    i++;
                    continue; // continue looking for the next colon (this one is inside a key or value)
                } else {
                    if (! isFirstNumberChar(input[i])) {
                        i++;
                        continue; // continue looking for the next colon (this one is inside a key or value)
                    }
                }
            }

            i = colonIndex - 1; // step back from the colon
            while (! isDoubleQuote(input[i])) {
                if (! isWhiteSpace(input[i])) {
                    // this is a colon in a JSON value, continue outer
                    i = i + 2;
                    continue outer;
                }
                i--; // loop back until index is on closing quote of JSON key
            }
            int closingQuoteIndex = i; // or opening double quote if colon was part of a JSON String

            if (i < 2) {
                // colon can only be in a JSON key
                i = colonIndex + 1;
                continue;
            }
            i--; // step back from closing quote
            while (! isDoubleQuote(input[i]) && ! isEscapeCharacter(input[i-1])) {
                i--; // loop back until index is on opening quote of key, so ignore escaped quotes (which are part of the value)
            }
            int openingQuoteIndex = i;
            int keyLength = closingQuoteIndex - openingQuoteIndex - 1; // quotes are not included, but it is a length, hence the minus 1
            byte[] keyBytes = new byte[keyLength];
            System.arraycopy(input, openingQuoteIndex + 1, keyBytes, 0, keyLength);
            String key = new String(keyBytes, StandardCharsets.UTF_8);
            i = colonIndex + 1; // continue looping from after colon
            if (!targetKeys.contains(key)) {
                i = i + 4; // +4 since minimum amount of characters between colon is 5: {"a":1,"":2}
                continue;
            }

            while (unrecognizedValueCharacter(input[i])) {
                if (isWhiteSpace(input[i])) {
                    i++; // skip white space characters
                    continue;
                }
                i++;
                // for any other character that isn't recognized to be a value (String or number in case number masking is enabled) we can skip to the other loop as there is nothing to mask
                continue outer;
            }
            int obfuscationLength = maskingConfig.getObfuscationLength();
            if (maskingConfig.isNumberMaskingEnabled() && isNumericCharacter(input[i])) {
                // This block deals with numeric values
                int targetValueLength = 0;
                while (isNumericCharacter(input[i])) {
                    targetValueLength++;
                    input[i] = Utf8AsciiCharacter.toUtf8ByteValue(maskingConfig.getMaskNumberValuesWith());
                    i++;
                    // Following line cannot result in ArrayOutOfBound because of the early return after checking for first char being a double quote

                }
                if (maskingConfig.isObfuscationEnabled() && obfuscationLength != targetValueLength) {
                    if (obfuscationLength == 0) {
                        // For obfuscation length 0, we want to obfuscate numeric values with a single 0 because an empty numeric value is illegal JSON
                        input = FixedLengthValueUtil.setFixedLengthOfValue(input, i, 1, targetValueLength, Utf8AsciiCharacter.toUtf8ByteValue(maskingConfig.getMaskNumberValuesWith()));
                    } else {
                        input = FixedLengthValueUtil.setFixedLengthOfValue(input, i, obfuscationLength, targetValueLength, Utf8AsciiCharacter.toUtf8ByteValue(maskingConfig.getMaskNumberValuesWith()));
                        i = i - (targetValueLength - obfuscationLength);
                    }
                }
            } else {
                // This block deals with Strings
                i++; // step over quote
                int targetValueLength = 0;
                int noOfEscapeCharacters = 0;
                boolean escapeNextCharacter = false;
                boolean previousCharacterCountedAsEscapeCharacter = false;
                while (! isDoubleQuote(input[i]) || (isDoubleQuote(input[i]) && escapeNextCharacter)) {
                    escapeNextCharacter = isEscapeCharacter(input[i]);
                    if (escapeNextCharacter && !previousCharacterCountedAsEscapeCharacter) {
                        noOfEscapeCharacters++;
                        previousCharacterCountedAsEscapeCharacter = true;
                    } else {
                        previousCharacterCountedAsEscapeCharacter = false;
                    }
                    input[i] = Utf8AsciiCharacter.ASTERISK.getUtf8ByteValue();
                    targetValueLength++;
                    i++;
                }
                if (maskingConfig.isObfuscationEnabled() && obfuscationLength != (targetValueLength - noOfEscapeCharacters)) {
                    input = FixedLengthValueUtil.setFixedLengthOfStringValue(input, i, obfuscationLength, targetValueLength); // set reference of input bytes to the new array reference
                    // compensate the index for shortening the input by setting fixed length to be at the closing double quote of the String value
                    i = i - (targetValueLength - obfuscationLength);
                }  else if (noOfEscapeCharacters > 0) { // So we don't add asterisks for escape characters (which are not part of the String length)
                    int actualStringLength = targetValueLength - noOfEscapeCharacters;
                    input = FixedLengthValueUtil.setFixedLengthOfStringValue(input, i, actualStringLength, targetValueLength);
                    // compensate the index for shortening the input with noOfEscapeCharacters to be at closing double quote of the String value
                    i = i - noOfEscapeCharacters;
                }
            }
            i++; // step over closing double quote
        }
        return input;
    }

    /**
     * Wrong character is anything that is not a quotation mark (denoting String value beginnings) or not numeric in case number masking is enabled.
     * @param inputByte the character to check
     * @return true if it's an unrecognized value character or else false
     */
    private boolean unrecognizedValueCharacter(byte inputByte) {
        if (maskingConfig.isNumberMaskingEnabled() && isNumericCharacter(inputByte)) {
            return false;
        }
        return Utf8AsciiCharacter.DOUBLE_QUOTE.getUtf8ByteValue() != inputByte;
    }

    @NotNull
    @Override
    public String mask(@NotNull String input) {
        return new String(mask(input.getBytes(StandardCharsets.UTF_8)),StandardCharsets.UTF_8);
    }

}
