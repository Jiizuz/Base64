package com.github.jiizuz.base64;

import java.util.Arrays;

/**
 * This class implements an encoder for encoding byte data using
 * the Base64 encoding scheme as specified in RFC 4648 and RFC 2045.
 *
 * <p> Instances of {@link Encoder} class are safe for use by
 * multiple concurrent threads.
 *
 * <p> Unless otherwise noted, passing a {@code null} argument to
 * a method of this class will cause a
 * {@link java.lang.NullPointerException NullPointerException} to
 * be thrown.
 *
 * @see Decoder
 * @since 1.0
 */
public class Encoder {

    /**
     * This array is a lookup table that translates 6-bit positive integer
     * index values into their "Base64 Alphabet" equivalents as specified
     * in "Table 1: The Base64 Alphabet" of RFC 2045 (and RFC 4648).
     */
    static final char[] TO_BASE_64 = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
    };

    /**
     * It's the lookup table for "URL and Filename safe Base64" as specified
     * in Table 2 of the RFC 4648, with the '+' and '/' changed to '-' and
     * '_'. This table is used when BASE64_URL is specified.
     */
    static final char[] TO_BASE_64_URL = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_'
    };

    private static final int MIME_LINE_MAX = 76;
    private static final byte[] CRLF = new byte[]{'\r', '\n'};

    static final Encoder RFC4648 = new Encoder(false, null, -1, true);
    static final Encoder RFC4648_URL_SAFE = new Encoder(true, null, -1, true);
    static final Encoder RFC2045 = new Encoder(false, CRLF, MIME_LINE_MAX, true);

    private final byte[] newline;
    private final int lineMax;
    private final boolean isURL;
    private final boolean doPadding;

    Encoder(final boolean isURL, final byte[] newline, final int lineMax, final boolean doPadding) {
        this.isURL = isURL;
        this.newline = newline;
        this.lineMax = lineMax;
        this.doPadding = doPadding;
    }

    private int outLength(final int sourceLength) {
        int length;
        if (doPadding) {
            length = 4 * ((sourceLength + 2) / 3);
        } else {
            final int n = sourceLength % 3;
            length = 4 * (sourceLength / 3) + (n == 0 ? 0 : n + 1);
        }
        if (lineMax > 0) { // sum line separators length
            length += (length - 1) / lineMax * newline.length;
        }
        return length;
    }

    /**
     * Encodes all bytes from the specified byte array into a newly-allocated
     * byte array using the {@link Base64} encoding scheme. The returned byte
     * array is of the length of the resulting bytes.
     *
     * @param source the byte array to encode
     * @return A newly-allocated byte array containing the resulting
     * encoded bytes.
     */
    public byte[] encode(final byte[] source) {
        final byte[] destiny = new byte[outLength(source.length)];
        final int ret = encode0(source, source.length, destiny);
        if (ret != destiny.length)
            return Arrays.copyOf(destiny, ret);
        return destiny;
    }

    /**
     * Encodes all bytes from the specified byte array using the
     * {@link Base64} encoding scheme, writing the resulting bytes to the
     * given output byte array, starting at offset 0.
     *
     * <p> It is the responsibility of the invoker of this method to make
     * sure the output byte array {@code destiny} has enough space for encoding
     * all bytes from the input byte array. No bytes will be written to the
     * output byte array if the output byte array is not big enough.
     *
     * @param source  the byte array to encode
     * @param destiny the output byte array
     * @return The number of bytes written to the output byte array
     * @throws IllegalArgumentException if {@code dst} does not have enough
     *                                  space for encoding all input bytes.
     */
    public int encode(final byte[] source, final byte[] destiny) {
        // destiny array size
        final int len = outLength(source.length);
        if (destiny.length < len)
            throw new IllegalArgumentException("Output byte array is too small for encoding all input bytes");
        return encode0(source, source.length, destiny);
    }

    /**
     * Returns an encoder instance that encodes equivalently to this one,
     * but without adding any padding character at the end of the encoded
     * byte data.
     *
     * <p> The encoding scheme of this encoder instance is unaffected by
     * this invocation. The returned encoder instance should be used for
     * non-padding encoding operation.
     *
     * @return an equivalent encoder that encodes without adding any
     * padding character at the end
     */
    public Encoder withoutPadding() {
        if (!doPadding)
            return this;
        return new Encoder(isURL, newline, lineMax, false);
    }

    private int encode0(final byte[] source, final int sourceLength, final byte[] destiny) {
        final char[] base64 = isURL ? TO_BASE_64_URL : TO_BASE_64;
        int maxSourceLength = sourceLength / 3 * 3;
        final int newSourceLength = maxSourceLength;
        if (lineMax > 0 && maxSourceLength > lineMax / 4 * 3)
            maxSourceLength = lineMax / 4 * 3;
        int sourcePosition = 0;
        int destinyPosition = 0;
        while (sourcePosition < newSourceLength) {
            int sl0 = Math.min(sourcePosition + maxSourceLength, newSourceLength);
            for (int sp0 = sourcePosition, dp0 = destinyPosition; sp0 < sl0; ) {
                final int bits = (source[sp0++] & 0xFF) << 16 | (source[sp0++] & 0xFF) << 8 | (source[sp0++] & 0xFF);
                destiny[dp0++] = (byte) base64[(bits >>> 18) & 0x3F];
                destiny[dp0++] = (byte) base64[(bits >>> 12) & 0x3F];
                destiny[dp0++] = (byte) base64[(bits >>> 6) & 0x3F];
                destiny[dp0++] = (byte) base64[bits & 0x3F];
            }
            final int destinyLength = (sl0 - sourcePosition) / 3 * 4;
            destinyPosition += destinyLength;
            sourcePosition = sl0;
            if (destinyLength == lineMax && sourcePosition < sourceLength) {
                for (final byte b : newline) {
                    destiny[destinyPosition++] = b;
                }
            }
        }
        // 1 or 2 leftover bytes
        if (sourcePosition < sourceLength) {
            final int b0 = source[sourcePosition++] & 0xff;
            destiny[destinyPosition++] = (byte) base64[b0 >> 2];
            if (sourcePosition == sourceLength) {
                destiny[destinyPosition++] = (byte) base64[(b0 << 4) & 0x3f];
                if (doPadding) {
                    destiny[destinyPosition++] = '=';
                    destiny[destinyPosition++] = '=';
                }
            } else {
                final int b1 = source[sourcePosition] & 0xff;
                destiny[destinyPosition++] = (byte) base64[(b0 << 4) & 0x3f | (b1 >> 4)];
                destiny[destinyPosition++] = (byte) base64[(b1 << 2) & 0x3f];
                if (doPadding) {
                    destiny[destinyPosition++] = '=';
                }
            }
        }
        return destinyPosition;
    }
}
