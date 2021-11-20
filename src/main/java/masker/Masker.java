package masker;

import java.nio.charset.StandardCharsets;

class Masker {
    public static void main(String[] args) {
        String input = "{\"ab\":\"value\"}";
        String maskedInput = "{\"ab\":\"*****\"}";
        String filterKey = "ab";
        System.out.println(maskValueOfKeyJson(input, filterKey));
        System.out.println(maskedInput.equals(maskValueOfKeyJson(input, filterKey)));
    }

    static String maskValueOfKeyJson(String input, String filterKey) {
        String filterJsonKey = "\"" + filterKey + "\"";
        int startIndexOfFilterKey = input.indexOf(filterJsonKey);
        if (startIndexOfFilterKey == -1) {
            return input; // input doesn't contain filter key, so no need to mask anything
        }

        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        int colonIndex = 0;
        int i = startIndexOfFilterKey + filterJsonKey.length();
        for (; i < inputBytes.length; i++) {
            if (inputBytes[i] == getByteValueOfUTF8String(":")) {
                colonIndex = i;
                break;
            } else if (inputBytes[i] == getByteValueOfUTF8String(" ")) {
                continue;
            } else {
                break;
            }
        }
        if (colonIndex == 0) {
            String newInput = input.substring(i);
            return maskValueOfKeyJson(newInput, filterKey); // input contained filter key, but it wasn't a JSON key, so continue on the tail
        }
        int jsonValueOpenQuoteIndex = 0;
        i++; // step over colon
        for (; i < inputBytes.length; i++) {
            if (inputBytes[i] == getByteValueOfUTF8String("\"")) {
                i++; // skip over quotes
                while(inputBytes[i] != getByteValueOfUTF8String("\"")) {
                    inputBytes[i] = getByteValueOfUTF8String("*");
                    i++;
                }
                break;
            } else if (inputBytes[i] == getByteValueOfUTF8String(" ")) {
                continue;
            } else {
                String newInput = input.substring(i);
                return maskValueOfKeyJson(newInput, filterKey);
            }
        }
        return new String(inputBytes, StandardCharsets.UTF_8);
    }

    static byte getByteValueOfUTF8String(String inputStringCharacter) {
        if (inputStringCharacter.length() != 1) {
            throw new IllegalArgumentException("This method should only be called for Strings which are only a single byte in UTF-8");
        }
        return inputStringCharacter.getBytes(StandardCharsets.UTF_8)[0];
    }
}