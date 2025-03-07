package dev.blaauwendraad.masker.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

/**
 * Represents the state of the {@link JsonMasker} at a given point in time during the {@link JsonMasker#mask(InputStream, OutputStream)} )}
 * operation.
 */
class BufferedMaskingState extends MaskingState {
    /**
     * Defines the maximum size for the buffer used by the streaming API:
     * {@link JsonMasker#mask(InputStream, OutputStream)}.
     * <p>
     * This is a security measure to prevent too much memory being allocated for maliciously crafted JSONs with huge
     * tokens (keys or values) to consume too much memory.
     * <p>
     * The maximum allowed buffer size corresponds to 16MB, which corresponds to a maximum token length of 4 million
     * characters.
     */
    private static final int MAX_BUFFER_SIZE = 16777216;
    private static final String STREAM_READ_ERROR_MESSAGE = "Failed to read from input stream";
    private static final String STREAM_WRITE_ERROR_MESSAGE = "Failed to write to output stream";

    private final InputStream inputStream;
    private final OutputStream outputStream;
    private int bufferSize; // size of byte array buffers to be read from the input stream

    public BufferedMaskingState(InputStream inputStream, OutputStream outputStream, int bufferSize, KeyMatcher.RadixTriePointer keyMatcherRootNodePointer) {
        super(new byte[bufferSize], keyMatcherRootNodePointer);
        /*
         There is a special optimization for "true", "false" and "null" values. We identify such values by their first
         character ("t", "f" and "n" respectively) and assume the identified value length. When the masker is in allow
         mode, we may step over these values. In case the buffer size is less than the maximum possible length of such a
         "special" value, we end up stepping over the entire buffer. To mitigate that, we force the minimum buffer size
         to be the maximum possible length of such "special" values, which is 5 (in "false").
        */
        if (bufferSize < 5) {
            throw new IllegalArgumentException("Buffer size must be at least 5 bytes");
        }
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.bufferSize = bufferSize;
        this.messageLength = 0;
        readNextBuffer();
    }

    @Override
    public boolean next() {
        return super.next() || reloadBuffer();
    }

    @Override
    public boolean endOfJson() {
        return super.endOfJson() && !reloadBuffer();
    }

    @Override
    public int byteLength() {
        if (messageLength <= currentIndex) {
            reloadBuffer();
        }
        return super.byteLength();
    }

    /**
     * Reads the next buffer and extends the buffer size if necessary.
     *
     * @throws UncheckedIOException if an I/O error occurs while reading from the input stream
     * @return {@code true} if more data is available in the stream, {@code false} otherwise
     */
    private boolean readNextBuffer() {
        if (!isCurrentTokenRegistered()) {
            // the pointer is not at a json value, so we are safe to read the next buffer
            currentIndex -= messageLength;
            try {
                messageLength = inputStream.readNBytes(message, 0, bufferSize);
            } catch (IOException e) {
                throw new UncheckedIOException(STREAM_READ_ERROR_MESSAGE, e);
            }
        } else {
            // the current buffer has ended before the masker finished processing the current value.
            int currentTokenLength = messageLength - currentTokenStartIndex;
            moveCurrentTokenToBeginningOfBuffer(currentTokenLength);

            // fill up the remaining of the buffer
            try {
                messageLength = inputStream.readNBytes(message, currentTokenLength, bufferSize - currentTokenLength) + currentTokenLength;
            } catch (IOException e) {
                throw new UncheckedIOException(STREAM_READ_ERROR_MESSAGE, e);
            }

            // reset pointers
            currentIndex -= currentTokenStartIndex;
            currentTokenStartIndex = 0;
        }
        return messageLength > currentIndex;
    }

    /**
     * Moves the current JSON token to the beginning of buffer.
     * <p>
     * If the current JSON token size is larger than a quarter of the buffer size, double the buffer size.
     *
     * @param currentTokenLength the length of the current JSON token, in bytes
     */
    private void moveCurrentTokenToBeginningOfBuffer(int currentTokenLength) {
        if (currentTokenLength < bufferSize >> 2) { // note: >> 2 is equal to dividing by 4
            // in case the current value is shorter than a quarter of the buffer fill up the buffer without extending its
            // length by moving the current value to the beginning of the buffer
            System.arraycopy(message, currentTokenStartIndex, message, 0, currentTokenLength);
        } else {
            // in case the current value is longer than a quarter of the buffer, double the buffer size
            bufferSize <<= 1; // note: <<= 1 is equal to doubling the bufferSize
            if (bufferSize > MAX_BUFFER_SIZE) {
                throw new InvalidJsonException(
                        String.format("Invalid JSON input provided: it contains a single JSON token (key or value) with %s characters", currentTokenLength));
            }
            byte[] extendedBuffer = new byte[bufferSize];

            // move the current value to the beginning of the extended buffer
            System.arraycopy(message, currentTokenStartIndex, extendedBuffer, 0, currentTokenLength);
            message = extendedBuffer;
        }
    }

    /**
     * Flushes the remaining of the current buffer up to the current token start index into the output stream
     *
     * @throws UncheckedIOException if an I/O error occurs while writing to the output stream
     */
    public void flushCurrentBuffer() {
        try {
            int remainingBufferLength = !isCurrentTokenRegistered() ?
                    messageLength - lastReplacementEndIndex : // flush the remaining of the message
                    currentTokenStartIndex - lastReplacementEndIndex; // flush the remaining of the message up to the current token start index
            outputStream.write(message, lastReplacementEndIndex, remainingBufferLength);
            outputStream.flush();
            lastReplacementEndIndex = 0;
        } catch (IOException e) {
            throw new UncheckedIOException(STREAM_READ_ERROR_MESSAGE, e);
        }
    }

    /**
     * Flushes the current buffer into the output stream, moves the current token to the beginning of the
     * buffer, and fills up the buffer from the input stream.
     * In case the current token is too long (i.e. the start index is not in the last quarter of the buffer),
     * double the current buffer size.
     *
     * @return {@code true} if more data is available in the input stream, {@code false} if the end of the
     * stream is reached.
     */
    private boolean reloadBuffer() {
        flushCurrentBuffer();
        return readNextBuffer();
    }

    @Override
    public void replaceTargetValueWith(int startIndex, int length, byte[] mask, int maskRepeat) {
        // write the replacement into the output stream
        try {
            // write everything up to the beginning of the current replacement
            outputStream.write(message, lastReplacementEndIndex, startIndex - lastReplacementEndIndex);

            // write the replacement
            for (int i = 0; i < maskRepeat; i++) {
                outputStream.write(mask);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(STREAM_WRITE_ERROR_MESSAGE, e);
        }
        lastReplacementEndIndex = startIndex + length;
    }
}
