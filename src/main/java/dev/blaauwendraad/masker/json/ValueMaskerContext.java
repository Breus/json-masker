package dev.blaauwendraad.masker.json;

public interface ValueMaskerContext {
    byte getByte(int index);

    int valueLength();

    void replaceValue(int fromIndex, int length, byte[] mask, int maskRepeat);

    int countNonVisibleCharacters(int fromIndex, int length);

    String asString();
}
