package randomgen.json;

import static dev.blaauwendraad.masker.json.util.AsciiCharacter.CARRIAGE_RETURN;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.HORIZONTAL_TAB;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.LINE_FEED;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.SPACE;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.isLowercaseA;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.isLowercaseE;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.isLowercaseL;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.isLowercaseR;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.isLowercaseS;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.isLowercaseU;

import dev.blaauwendraad.masker.json.util.AsciiCharacter;
import dev.blaauwendraad.masker.json.util.AsciiJsonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;

public class RandomJsonWhiteSpaceInjector {
    private final byte[] originalJsonBytes;
    private final int maxNumberOfWhiteSpacesToInject;

    public RandomJsonWhiteSpaceInjector(byte[] originalJsonBytes, int maxNumberOfWhiteSpacesToInject) {
        this.originalJsonBytes = originalJsonBytes;
        this.maxNumberOfWhiteSpacesToInject = maxNumberOfWhiteSpacesToInject;
    }

    public byte[] getWhiteSpaceInjectedJson() {
        return insertRandomValidWhitespaces();
    }

    @Nonnull
    private byte[] insertRandomValidWhitespaces() {
        final List<Integer> randomWhitespaceInjectionIndexes = new ArrayList<>();
        // We inject at least 1 and at most 50 random whitespaces in the original JSON
        int maxNumberOfWhiteSpacesLeftToInject = Math.min(originalJsonBytes.length / 2, 50);
        while (maxNumberOfWhiteSpacesLeftToInject > 0) {
            int randomIndex = new Random().nextInt(1, originalJsonBytes.length);
            if (canInjectWhiteSpaceBeforeByteAtIndex(originalJsonBytes, randomIndex)) {
                randomWhitespaceInjectionIndexes.add(randomIndex);
            }
            maxNumberOfWhiteSpacesLeftToInject--;
        }
        return insertRandomWhiteSpaces(randomWhitespaceInjectionIndexes, originalJsonBytes);
    }

    @Nonnull
    private static byte[] insertRandomWhiteSpaces(List<Integer> indexesToInsertWhiteSpaces, byte[] originalJsonBytes) {
        final byte[] resultingJsonBytes = new byte[indexesToInsertWhiteSpaces.size() + originalJsonBytes.length];
        indexesToInsertWhiteSpaces.sort((a, b) -> Integer.compare(a, b));
        int indexInOriginalByteArray = 0;
        int indexInResultByteArray = 0;
        int whiteSpacesInjectedSoFar = 0;
        while (!indexesToInsertWhiteSpaces.isEmpty()) {
            int indexToInsert = indexesToInsertWhiteSpaces.get(0);
            System.arraycopy(
                    originalJsonBytes,
                    indexInOriginalByteArray,
                    resultingJsonBytes,
                    indexInResultByteArray,
                    indexToInsert - indexInOriginalByteArray);
            resultingJsonBytes[indexToInsert + whiteSpacesInjectedSoFar] = getRandomWhiteSpaceByte();
            whiteSpacesInjectedSoFar++;
            indexInOriginalByteArray = indexToInsert;
            indexInResultByteArray = indexToInsert + whiteSpacesInjectedSoFar;
            indexesToInsertWhiteSpaces.remove(0);
        }
        System.arraycopy(
                originalJsonBytes,
                indexInOriginalByteArray,
                resultingJsonBytes,
                indexInResultByteArray,
                originalJsonBytes.length - indexInOriginalByteArray);
        return resultingJsonBytes;
    }

    private static byte getRandomWhiteSpaceByte() {
        int randomInt = new Random().nextInt(4);
        return switch (randomInt) {
            case 0 -> CARRIAGE_RETURN.getAsciiByteValue();
            case 1 -> HORIZONTAL_TAB.getAsciiByteValue();
            case 2 -> LINE_FEED.getAsciiByteValue();
            case 3 -> SPACE.getAsciiByteValue();
            default -> throw new IllegalStateException("Unexpected value to get random whitespace byte: " + randomInt);
        };
    }

    /**
     * Checks whether a whitespace can be injected before the provided index in the provided JsonBytes.
     * To keep it simple, this method is sometimes stricter than it needs to be (it returns false
     * while it was possible, for example in Strings). However, it will never return true if that
     * would result in invalid JSON if a whitespace would be injected before the provided index.
     *
     * @param jsonBytes the JSON bytes for which it will be checked if the whitespace can be
     *     injected
     * @param index the index for which it will be checked, must be > 1
     * @return whether injecting a whitespace before the index in the provided JSON bytes will
     *     result in valid JSON
     */
    private static boolean canInjectWhiteSpaceBeforeByteAtIndex(byte[] jsonBytes, int index) {
        byte originalJsonByte = jsonBytes[index];
        Byte previousOriginalByte = null;
        if (index > 0) {
            previousOriginalByte = jsonBytes[index - 1];
        }
        // Not allowed in null values
        if (isLowercaseU(originalJsonByte) || isLowercaseL(originalJsonByte)) {
            return false;
        }
        // Not allowed in numeric values
        if (AsciiJsonUtil.isNumericCharacter(originalJsonByte)
                && previousOriginalByte != null
                && AsciiJsonUtil.isNumericCharacter(previousOriginalByte)) {
            return false;
        }
        // Not allowed in boolean values (true/false)
        if (isLowercaseA(originalJsonByte)
                || isLowercaseL(originalJsonByte)
                || isLowercaseS(originalJsonByte)
                || isLowercaseE(originalJsonByte)
                || isLowercaseR(originalJsonByte)
                || isLowercaseU(originalJsonByte)) {
            return false;
        }
        // Adding whitespaces in between escaped character and the escape character can result in invalid JSON
        if (previousOriginalByte != null && AsciiCharacter.isEscapeCharacter(previousOriginalByte)) {
            return false;
        }
        return true;
    }
}
