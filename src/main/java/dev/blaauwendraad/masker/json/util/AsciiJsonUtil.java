package dev.blaauwendraad.masker.json.util;

import static dev.blaauwendraad.masker.json.util.AsciiCharacter.CARRIAGE_RETURN;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.HORIZONTAL_TAB;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.LINE_FEED;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.SPACE;

public final class AsciiJsonUtil {

    private AsciiJsonUtil() {
        // don't instantiate
    }

    public static boolean isFirstNumberChar(byte utf8Character) {
        return switch (utf8Character) {
            case '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> true;
            default -> false;
        };
    }

    public static boolean isNumericCharacter(byte utf8Character) {
        return switch (utf8Character) {
            case '-', '+', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', 'e', 'E' -> true;
            default -> false;
        };
    }

}
