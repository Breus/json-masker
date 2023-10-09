package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;

/**
 * (NOT YET IMPLEMENTED) JSONPath-aware {@link JsonMasker}.
 */
public class PathAwareKeyContainsMasker implements JsonMasker {
    private final JsonMaskingConfig maskingConfig;

    /**
     * Creates an instance of the JSONPath-aware {@link JsonMasker}
     *
     * @param maskingConfig the masking configurations for the created masker
     */
    public PathAwareKeyContainsMasker(JsonMaskingConfig maskingConfig) {
        this.maskingConfig = maskingConfig;
    }

    @Override
    public byte[] mask(byte[] input) {
        // TODO: implement algorithm
        throw new UnsupportedOperationException("JSONPaths are not yet supported");
    }
}
