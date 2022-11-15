package masker.json;

public record JsonMaskerTestInstance(
        String input,
        String expectedOutput,
        JsonMasker jsonMasker
) {
    @Override
    public String toString() {
        return jsonMasker.getClass().getSimpleName() + ": input='" + input + '\'' +
                ", expectedOutput='" + expectedOutput + '\'';
    }
}