package dev.blaauwendraad.masker.json;

import org.jspecify.annotations.Nullable;

/**
 * The state of JsonMasker that keeps reference to the input byte array and proves API for its manipulation.
 */
sealed interface MaskingState extends ValueMaskerContext permits ByteArrayMaskingState, BufferedMaskingState {
    boolean jsonPathEnabled();
    void expandCurrentJsonPath(KeyMatcher.@Nullable TrieNode trieNode);
    byte[] getMessage();
    byte[] flushReplacementOperations();
    KeyMatcher.@Nullable TrieNode getCurrentJsonPathNode();
    boolean endOfJson();
    byte byteAtCurrentIndex();
    void incrementIndex(int length);
    boolean next();
    void backtrackCurrentJsonPath();
    void registerValueStartIndex();
    int currentIndex();
    int getCurrentValueStartIndex();
    void clearValueStartIndex();


}
