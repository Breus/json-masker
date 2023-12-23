package dev.blaauwendraad.masker.json;

/**
 * Represents the state of the {@link JsonMasker} at a given point in time during the {@link JsonMasker#mask(byte[])}
 * operation.
 */
public final class MaskingState {
    private byte[] message;
    private int currentIndex;

    public MaskingState(byte[] message, int currentIndex) {
        this.message = message;
        this.currentIndex = currentIndex;
    }

    public void incrementCurrentIndex() {
        currentIndex++;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public void setMessage(byte[] newMessage) {
        this.message = newMessage;
    }

    public byte byteAtCurrentIndex() {
        return message[currentIndex];
    }

    public void setByteAtCurrentIndex(byte newByte) {
        message[currentIndex] = newByte;
    }

    public byte byteAtCurrentIndexMinusOne() {
        return message[currentIndex - 1];
    }

    public int currentIndex() {
        return currentIndex;
    }

    public int messageLength() {
        return message.length;
    }

    public byte[] getMessage() {
        return message;
    }

    // for debugging purposes, shows the current state of message traversal
    public String peek() {
        return "current: '" + (currentIndex == message.length ? "<end of json>" : (char) message[currentIndex]) + "'," +
                " before: '" + new String(message, Math.max(0, currentIndex - 10), Math.min(10, currentIndex)) + "'," +
                " after: '" + new String(message, currentIndex, Math.min(10, message.length - currentIndex)) + "'";
    }
}
