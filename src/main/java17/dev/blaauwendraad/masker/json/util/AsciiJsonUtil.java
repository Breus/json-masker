package dev.blaauwendraad.masker.json.util;

public final class AsciiJsonUtil {

    private AsciiJsonUtil() { /* don't instantiate */ }

    public static boolean isWhiteSpace(byte utf8Character) {
        return switch (utf8Character) {
            case '\n', '\t', '\r', ' ' -> true;
            default -> false;
        };
    }

    public static boolean isNumericCharacter(byte utf8Character) {
        return switch (utf8Character) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '+', '.', 'e', 'E' -> true;
            default -> false;
        };
    }

}
