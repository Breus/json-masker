package dev.blaauwendraad.masker.json.util;

import static dev.blaauwendraad.masker.json.util.AsciiCharacter.CARRIAGE_RETURN;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.HORIZONTAL_TAB;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.LINE_FEED;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.LOWERCASE_E;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.MINUS;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.NINE;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.PERIOD;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.PLUS;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.SPACE;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.UPPERCASE_E;
import static dev.blaauwendraad.masker.json.util.AsciiCharacter.ZERO;

public final class AsciiJsonUtil {

    private AsciiJsonUtil() {
        // don't instantiate
    }
    public static boolean isWhiteSpace(byte utf8Character) {
        return utf8Character == CARRIAGE_RETURN.getAsciiByteValue()
                || utf8Character == HORIZONTAL_TAB.getAsciiByteValue()
                || utf8Character == LINE_FEED.getAsciiByteValue()
                || utf8Character == SPACE.getAsciiByteValue();
    }

    public static boolean isFirstNumberChar(byte utf8Character) {
        return (utf8Character >= ZERO.getAsciiByteValue() && utf8Character <= NINE.getAsciiByteValue())
                || utf8Character == MINUS.getAsciiByteValue();
    }

    public static boolean isNumericCharacter(byte utf8Character) {
        return (utf8Character >= ZERO.getAsciiByteValue() && utf8Character <= NINE.getAsciiByteValue())
                || utf8Character == MINUS.getAsciiByteValue()
                || utf8Character == PLUS.getAsciiByteValue()
                || utf8Character == PERIOD.getAsciiByteValue()
                || utf8Character == LOWERCASE_E.getAsciiByteValue()
                || utf8Character == UPPERCASE_E.getAsciiByteValue();
    }

}
