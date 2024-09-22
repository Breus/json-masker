package dev.blaauwendraad.masker.json.config;

/**
 * Utility class to configure internal configuration of JsonMaskingConfig
 */
public final class JsonMaskingConfigTestUtil {
    public static void setBufferSize(JsonMaskingConfig jsonMaskingConfig, int bufferSize) {
        jsonMaskingConfig.bufferSize = bufferSize;
    }
}
