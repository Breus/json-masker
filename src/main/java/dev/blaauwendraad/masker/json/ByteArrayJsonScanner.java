package dev.blaauwendraad.masker.json;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

/** Bulk byte-array searches used by the JSON scanner. */
final class ByteArrayJsonScanner {
    private static final VarHandle LONG_VIEW =
            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.nativeOrder());
    private static final long QUOTE_BYTES = 0x2222222222222222L;
    private static final long BACKSLASH_BYTES = 0x5c5c5c5c5c5c5c5cL;
    private static final long LOW_BITS = 0x0101010101010101L;
    private static final long HIGH_BITS = 0x8080808080808080L;

    private ByteArrayJsonScanner() {}

    /** Returns the first quote or backslash in {@code [fromIndex, toIndex)}, or {@code toIndex} if none exists. */
    static int indexOfQuoteOrBackslash(byte[] bytes, int fromIndex, int toIndex) {
        int index = fromIndex;
        int bulkEnd = toIndex - Long.BYTES;
        while (index <= bulkEnd) {
            long word = (long) LONG_VIEW.get(bytes, index);
            if (containsZeroByte(word ^ QUOTE_BYTES) || containsZeroByte(word ^ BACKSLASH_BYTES)) {
                int wordEnd = index + Long.BYTES;
                while (index < wordEnd) {
                    byte value = bytes[index];
                    if (value == '"' || value == '\\') {
                        return index;
                    }
                    index++;
                }
            } else {
                index += Long.BYTES;
            }
        }
        while (index < toIndex) {
            byte value = bytes[index];
            if (value == '"' || value == '\\') {
                return index;
            }
            index++;
        }
        return toIndex;
    }

    private static boolean containsZeroByte(long value) {
        return ((value - LOW_BITS) & ~value & HIGH_BITS) != 0;
    }
}
