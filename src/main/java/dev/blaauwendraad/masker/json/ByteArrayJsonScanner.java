package dev.blaauwendraad.masker.json;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

/** Bulk byte-array searches used by the JSON scanner. */
final class ByteArrayJsonScanner {
    private static final VarHandle LONG_VIEW =
            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.nativeOrder());
    private static final long QUOTE_BYTES = 0x2222222222222222L;
    private static final long LOW_BITS = 0x0101010101010101L;
    private static final long HIGH_BITS = 0x8080808080808080L;

    private ByteArrayJsonScanner() {}

    /** Returns the first quote in {@code [fromIndex, toIndex)}, or {@code toIndex} if none exists. */
    static int indexOfQuote(byte[] bytes, int fromIndex, int toIndex) {
        int index = fromIndex;
        int bulkEnd = toIndex - Long.BYTES;
        while (index <= bulkEnd) {
            long word = (long) LONG_VIEW.get(bytes, index);
            if (containsZeroByte(word ^ QUOTE_BYTES)) {
                int wordEnd = index + Long.BYTES;
                while (index < wordEnd) {
                    if (bytes[index] == '"') {
                        return index;
                    }
                    index++;
                }
            } else {
                index += Long.BYTES;
            }
        }
        while (index < toIndex) {
            if (bytes[index] == '"') {
                return index;
            }
            index++;
        }
        return toIndex;
    }

    /** Checks the backslash run ending immediately before {@code endIndex}. */
    static boolean isPrecededByOddBackslashRun(
            byte[] bytes, int fromIndex, int endIndex, boolean oddRunBeforeFromIndex) {
        boolean odd = false;
        int index = endIndex - 1;
        while (index >= fromIndex && bytes[index] == '\\') {
            odd = !odd;
            index--;
        }
        return odd ^ (index < fromIndex && oddRunBeforeFromIndex);
    }

    private static boolean containsZeroByte(long value) {
        return ((value - LOW_BITS) & ~value & HIGH_BITS) != 0;
    }
}
