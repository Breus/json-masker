package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;

public class PathAwareKeyContainsMasker implements JsonMasker {
    private JsonMaskingConfig jsonMaskingConfig;

    public PathAwareKeyContainsMasker(JsonMaskingConfig jsonMaskingConfig) {
        this.jsonMaskingConfig = jsonMaskingConfig;
    }

    @Override
    public byte[] mask(byte[] input) {
        // TODO: implement algorithm
        return new byte[0];
    }
}
