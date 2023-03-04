package masker.json;

import masker.json.config.JsonMaskingConfig;

public class PathAwareKeyContainsMasker implements JsonMasker {
    public PathAwareKeyContainsMasker(JsonMaskingConfig jsonMaskingConfig) {
    }

    @Override
    public byte[] mask(byte[] input) {
        // TODO: implement algorithm
        return new byte[0];
    }
}
