package dev.blaauwendraad.masker.json;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

/**
 * A wrapper around {@code dev.blaauwendraad.masker.json.ByteArrayMaskingState} that reads JSON data from provided
 * input stream and flushes masked data into output stream. The implementation uses ByteArrayMasking state to hold
 * the current buffer. As soon as the end of the current buffer is reached, BufferedMaskingState reads a new chunk of data
 * and manipulates ByteArrayMaskingState accordingly.
 */
final class BufferedMaskingState implements MaskingState {
    private static final int BUFFER_SIZE = 8192; // size of byte array buffers to be read from the input stream
    private static final String READ_ERROR_MESSAGE = "Failed to read input stream";
    private static final String WRITE_ERROR_MESSAGE = "Failed to write to output stream";

    private final ByteArrayMaskingState delegate;
    private final InputStream inputStream;
    private final OutputStream outputStream;

    public BufferedMaskingState(@NonNull InputStream inputStream, @NonNull OutputStream outputStream, boolean trackJsonPath) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.delegate = new ByteArrayMaskingState(new byte[0], trackJsonPath);
        readNextBuffer();
    }

    /**
     * Reads next buffer from provided InputStream or extends the current buffer in case it finished while processing
     * a JSON value.
     *
     * @return true if more data is available in the input stream, false if the end of the stream is reached.
     */
    boolean readNextBuffer() {
        // check if it is the end of stream
        try {
            if (inputStream.available() == 0) {
                return false;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(READ_ERROR_MESSAGE, e);
        }
        // flush output of the current buffer
        flushReplacementOperations();

        if (delegate.currentValueStartIndex == -1) {
            // the pointer is not at a json value, so we are safe to read the next buffer
            delegate.currentIndex -= delegate.message.length;
            try {
                delegate.message = this.inputStream.readNBytes(BUFFER_SIZE);
            } catch (IOException e) {
                throw new UncheckedIOException(READ_ERROR_MESSAGE, e);
            }
        } else {
            // the current buffer has ended before the masker finished processing the current value.
            // cut everything before currentValueStartIndex and extend the current buffer instead of reading the next one.

            // read an extension to the current buffer
            byte[] extension;
            try {
                extension = this.inputStream.readNBytes(BUFFER_SIZE);
            } catch (IOException e) {
                throw new IllegalStateException(READ_ERROR_MESSAGE, e);
            }

            // cut everything before the start index of the current in-process value in the current buffer
            int cutLength = delegate.message.length - delegate.currentValueStartIndex;

            // copy the cut current buffer and extension into a single byte array
            byte[] extendedBuffer = new byte[cutLength + extension.length];
            System.arraycopy(delegate.message, delegate.currentValueStartIndex, extendedBuffer, 0, cutLength);
            System.arraycopy(extension, 0, extendedBuffer, cutLength, extension.length);
            delegate.message = extendedBuffer;

            // reset pointers
            delegate.currentIndex -= delegate.currentValueStartIndex;
            delegate.currentValueStartIndex = 0;
        }
        return true;
    }


    @Override
    public boolean jsonPathEnabled() {
        return delegate.jsonPathEnabled();
    }

    @Override
    public void expandCurrentJsonPath(KeyMatcher.@Nullable TrieNode trieNode) {
        delegate.expandCurrentJsonPath(trieNode);
    }

    @Override
    public byte[] getMessage() {
        return delegate.getMessage();
    }

    /**
     * Prepares a byte array of current replacement operations and flushes them into the output stream
     * @return flushed byte array
     */
    @Override
    public byte[] flushReplacementOperations() {
        // in case the current buffer has ended before the masker has finished processing the current value,
        // determine the length of the current value
        int currentValueLength = 0;
        if (delegate.currentValueStartIndex != -1) {
            currentValueLength = delegate.message.length - delegate.currentValueStartIndex;
        }

        // prepare the output replacement byte array
        byte[] newMessage = delegate.flushReplacementOperations();

        // write everything to the output stream except for the current in-process value
        try {
            outputStream.write(newMessage, 0, newMessage.length - currentValueLength);
            outputStream.flush();
        } catch (IOException e) {
            throw new IllegalStateException(WRITE_ERROR_MESSAGE, e);
        }

        // reset replacement operations state
        delegate.replacementOperations.clear();
        delegate.replacementOperationsTotalDifference = 0;
        return newMessage;
    }

    @Override
    public KeyMatcher.@Nullable TrieNode getCurrentJsonPathNode() {
        return delegate.getCurrentJsonPathNode();
    }

    @Override
    public boolean endOfJson() {
        if (delegate.endOfJson()) {
            readNextBuffer();
        }
        return delegate.endOfJson();
    }

    @Override
    public byte byteAtCurrentIndex() {
        return delegate.byteAtCurrentIndex();
    }

    @Override
    public void incrementIndex(int length) {
        delegate.incrementIndex(length);
    }

    @Override
    public boolean next() {
        if (!delegate.next()) {
            return readNextBuffer();
        }
        return true;
    }

    @Override
    public void backtrackCurrentJsonPath() {
        delegate.backtrackCurrentJsonPath();
    }

    @Override
    public void registerValueStartIndex() {
        delegate.registerValueStartIndex();
    }

    @Override
    public int currentIndex() {
        return delegate.currentIndex;
    }

    @Override
    public int getCurrentValueStartIndex() {
        return delegate.getCurrentValueStartIndex();
    }

    @Override
    public void clearValueStartIndex() {
        delegate.clearValueStartIndex();
    }

    @Override
    public byte getByte(int index) {
        return delegate.getByte(index);
    }

    @Override
    public int byteLength() {
        if (delegate.endOfJson()) {
            readNextBuffer();
        }
        return delegate.byteLength();
    }

    @Override
    public void replaceBytes(int fromIndex, int length, byte[] mask, int maskRepeat) {
        delegate.replaceBytes(fromIndex, length, mask, maskRepeat);
    }

    @Override
    public int countNonVisibleCharacters(int fromIndex, int length) {
        return delegate.countNonVisibleCharacters(fromIndex, length);
    }

    @Override
    public String asString(int fromIndex, int length) {
        return delegate.asString(fromIndex, length);
    }

    @Override
    public InvalidJsonException invalidJson(String message, int index) {
        return delegate.invalidJson(message, index);
    }
}
