package masker.json;

public class PathAwareKeyContainsMasker implements JsonMasker {
    public PathAwareKeyContainsMasker(JsonMaskingConfig jsonMaskingConfig) {
    }

    @Override
    public byte[] mask(byte[] input) {
        return new byte[0];
    }
}
