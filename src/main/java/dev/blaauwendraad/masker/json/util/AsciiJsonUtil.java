package dev.blaauwendraad.masker.json.util;

public final class AsciiJsonUtil {

    private AsciiJsonUtil() { /* don't instantiate */ }

    public static boolean isWhiteSpace(byte utf8Character) {
        switch (utf8Character) {
            case '\n':
            case '\t':
            case '\r':
            case ' ':
                return true;
            default:
                return false;
        }
    }

    public static boolean isNumericCharacter(byte utf8Character) {
        switch (utf8Character) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '-':
            case '+':
            case '.':
            case 'e':
            case 'E':
                return true;
            default:
                return false;
        }
    }

}
