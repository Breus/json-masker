package masker.json;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class JsonStringCharacters {
    private JsonStringCharacters() {
        // util
    }

    private static final Set<Character> asciiDigits = IntStream.rangeClosed(32, 57).mapToObj(i -> (char) i).collect(Collectors.toSet());
    private static final Set<Character> asciiSpecialChars1 = IntStream.rangeClosed(58,64).mapToObj(i -> (char) i).collect(Collectors.toSet());
    private static final Set<Character> asciiUppercaseLetters = IntStream.rangeClosed(65, 90).mapToObj(i -> (char) i).collect(Collectors.toSet());
    private static final Set<Character> asciiSpecialChars2 = IntStream.rangeClosed(91, 96).filter(i -> i != 92 /* escape character */).mapToObj(i -> (char) i).collect(Collectors.toSet());
    private static final Set<Character> asciiLowercaseLetters = IntStream.rangeClosed(97, 122).mapToObj(i -> (char) i).collect(Collectors.toSet());
    private static final Set<Character> asciiSpecialChars3 = IntStream.rangeClosed(123, 126).mapToObj(i -> (char) i).collect(Collectors.toSet());
    private static final Set<Character> unicodeControlCharacters = Set.of(
            '\u0000',
            '\u0001',
            '\u0002',
            '\u0003',
            '\u0004',
            '\u0005',
            '\u0006',
            '\u0007',
            '\u0008',
            '\u0009',
            '\n',
            '\u000B',
            '\u000C',
            '\r',
            '\u000E',
            '\u000F',
            '\u0010',
            '\u0011',
            '\u0012',
            '\u0013',
            '\u0014',
            '\u0015',
            '\u0016',
            '\u0017',
            '\u0018',
            '\u0019',
            '\u001A',
            '\u001B',
            '\u001C',
            '\u001D',
            '\u001E',
            '\u001F',
            '\u007F',
            '\u0080',
            '\u0081',
            '\u0083',
            '\u0084',
            '\u0085',
            '\u0086',
            '\u0087',
            '\u0088',
            '\u0089',
            '\u008A',
            '\u008B',
            '\u008C',
            '\u008D',
            '\u008E',
            '\u008F',
            '\u0090',
            '\u0091',
            '\u0092',
            '\u0093',
            '\u0094',
            '\u0095',
            '\u0096',
            '\u0097',
            '\u0098',
            '\u0099',
            '\u009A',
            '\u009B',
            '\u009C',
            '\u009D',
            '\u009E',
            '\u009F'
            );

    private static final Set<Character> randomPrintableUnicodeCharacters = Set.of(
            '\u0110',
            '\u0111',
            '\u0112',
            '\u0113',
            '\u0114',
            '\u0115',
            '\u0116',
            '\u1017',
            '\u1018',
            '\u1019',
            '\u101A',
            '\u101B',
            '\u101C',
            '\u101D',
            '\u201E',
            '\u201F',
            '\u207F',
            '\u2080',
            '\u2081',
            '\u9083',
            '\u9084',
            '\u9085',
            '\u9086',
            '\u9087'
    );

    public static Set<Character> getPrintableAsciiCharacters() {
        Set<Character> printableAsciiChars = new HashSet<>();
        printableAsciiChars.addAll(asciiDigits);
        printableAsciiChars.addAll(asciiSpecialChars1);
        printableAsciiChars.addAll(asciiUppercaseLetters);
        printableAsciiChars.addAll(asciiSpecialChars2);
        printableAsciiChars.addAll(asciiLowercaseLetters);
        printableAsciiChars.addAll(asciiSpecialChars3);
        return printableAsciiChars;
    }

    public static Set<Character> getUnicodeControlCharacters() {
        return unicodeControlCharacters;
    }

    public static Set<Character> getRandomPrintableUnicodeCharacters() {
        return randomPrintableUnicodeCharacters;
    }

    @SafeVarargs
    public static Set<Character> mergeCharSets(Set<Character>... charSet){
        Set<Character> mergedSet = new HashSet<>();
        for (Set<Character> characters : charSet) {
            mergedSet.addAll(characters);
        }
        return mergedSet;
    }
}
