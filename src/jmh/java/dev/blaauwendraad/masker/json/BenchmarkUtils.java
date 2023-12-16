package dev.blaauwendraad.masker.json;

import java.util.regex.Pattern;

public class BenchmarkUtils {

    public static int parseSize(String size) {
        // use regex to parse the jsonSize param
        Pattern pattern = Pattern.compile("^(\\d+)(\\w+)$");
        var matcher = pattern.matcher(size);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid size param: " + size);
        }
        int sizeBytes = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2).toLowerCase();
        switch (unit) {
            case "kb":
                sizeBytes *= 1024;
                break;
            case "mb":
                sizeBytes *= 1024 * 1024;
                break;
            default:
                throw new IllegalArgumentException("Invalid size param: " + size);
        }
        return sizeBytes;
    }
}
