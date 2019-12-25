package com.github.jiizuz.base64;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * This class implements a decoder for decoding byte data using the
 * Base64 encoding scheme as specified in RFC 4648 and RFC 2045.
 *
 * <p> The Base64 padding character {@code '='} is accepted and
 * interpreted as the end of the encoded byte data, but is not
 * required. So if the final unit of the encoded byte data only has
 * two or three Base64 characters (without the corresponding padding
 * character(s) padded), they are decoded as if followed by padding
 * character(s). If there is a padding character present in the
 * final unit, the correct number of padding character(s) must be
 * present, otherwise {@code IllegalArgumentException} (
 * {@code IOException} when reading from a Base64 stream) is thrown
 * during decoding.
 *
 * <p> Instances of {@link Decoder} class are safe for use by
 * multiple concurrent threads.
 *
 * <p> Unless otherwise noted, passing a {@code null} argument to
 * a method of this class will cause a
 * {@link java.lang.NullPointerException NullPointerException} to
 * be thrown.
 *
 * @see Encoder
 * @since 1.0
 */
public class Decoder {

    /**
     * Lookup table for decoding unicode characters drawn from the
     * "Base64 Alphabet" (as specified in Table 1 of RFC 2045) into
     * their 6-bit positive integer equivalents.  Characters that
     * are not in the Base64 alphabet but fall within the bounds of
     * the array are encoded to -1.
     */
    static final int[] FROM_BASE_64 = new int[256];

    static {
        Arrays.fill(FROM_BASE_64, -1);
        for (int i = 0; i < Encoder.TO_BASE_64.length; i++)
            FROM_BASE_64[Encoder.TO_BASE_64[i]] = i;
        FROM_BASE_64['='] = -2;
    }

    /**
     * Lookup table for decoding "URL and Filename safe Base64 Alphabet"
     * as specified in Table2 of the RFC 4648.
     */
    private static final int[] FROM_BASE_64_URL = new int[256];

    static {
        Arrays.fill(FROM_BASE_64_URL, -1);
        for (int i = 0; i < Encoder.TO_BASE_64_URL.length; i++)
            FROM_BASE_64_URL[Encoder.TO_BASE_64_URL[i]] = i;
        FROM_BASE_64_URL['='] = -2;
    }

    static final Decoder RFC4648 = new Decoder(false, false);
    static final Decoder RFC4648_URL_SAFE = new Decoder(true, false);
    static final Decoder RFC2045 = new Decoder(false, true);

    private final boolean isURL;
    private final boolean isMIME;

    private Decoder(final boolean isURL, final boolean isMIME) {
        this.isURL = isURL;
        this.isMIME = isMIME;
    }

    private int outLength(final byte[] source, final int sourceLength) {
        final int[] base64 = isURL ? FROM_BASE_64_URL : FROM_BASE_64;
        if (sourceLength < 2) {
            if (sourceLength == 0 || (isMIME && base64[0] == -1))
                return 0;
            throw new IllegalArgumentException("Input byte[] should at least have 2 bytes for base64 bytes");
        }
        int len = sourceLength;
        int padding = 0;
        if (isMIME) {
            // scan all bytes to fill out all non-alphabet. a performance
            // trade-off of pre-scan or Arrays.copyOf
            int n = 0;
            int sp = 0;
            while (sp < sourceLength) {
                final int b = source[sp++] & 0xFF;
                if (b == '=') {
                    len -= (sourceLength - sp + 1);
                    break;
                }
                if (base64[b] == -1)
                    n++;
            }
            len -= n;
        } else {
            if (source[sourceLength - 1] == '=') {
                padding++;
                if (source[sourceLength - 2] == '=')
                    padding++;
            }
        }
        if (padding == 0 && (len & 0x03) != 0)
            padding = 4 - (len & 0x03);
        return 3 * ((len + 3) >> 2) - padding;
    }

    /**
     * Decodes all bytes from the input byte array using the {@link Base64}
     * encoding scheme, writing the results into a newly-allocated output
     * byte array. The returned byte array is of the length of the resulting
     * bytes.
     *
     * @param source the byte array to decode
     * @return A newly-allocated byte array containing the decoded bytes.
     * @throws IllegalArgumentException if {@code source} is not in valid Base64 scheme
     */
    public byte[] decode(final byte[] source) {
        byte[] destiny = new byte[outLength(source, source.length)];
        final int ret = decode0(source, source.length, destiny);
        if (ret != destiny.length)
            destiny = Arrays.copyOf(destiny, ret);
        return destiny;
    }

    /**
     * Decodes a Base64 encoded String into a newly-allocated byte array
     * using the {@link Base64} encoding scheme.
     *
     * <p> An invocation of this method has exactly the same effect as invoking
     * {@code decode(source.getBytes(StandardCharsets.ISO_8859_1))}
     *
     * @param source the string to decode
     * @return A newly-allocated byte array containing the decoded bytes.
     * @throws IllegalArgumentException if {@code source} is not in valid Base64 scheme
     */
    public byte[] decode(final String source) {
        return decode(source.getBytes(StandardCharsets.ISO_8859_1));
    }

    /**
     * Decodes all bytes from the input byte array using the {@link Base64}
     * encoding scheme, writing the results into the given output byte array,
     * starting at offset 0.
     *
     * <p> It is the responsibility of the invoker of this method to make
     * sure the output byte array {@code destiny} has enough space for decoding
     * all bytes from the input byte array. No bytes will be be written to
     * the output byte array if the output byte array is not big enough.
     *
     * <p> If the input byte array is not in valid Base64 encoding scheme
     * then some bytes may have been written to the output byte array before
     * IllegalArgumentException is thrown.
     *
     * @param source  the byte array to decode
     * @param destiny the output byte array
     * @return The number of bytes written to the output byte array
     * @throws IllegalArgumentException if {@code source} is not in valid Base64 scheme, or {@code destiny}
     *                                  does not have enough space for decoding all input bytes.
     */
    public int decode(final byte[] source, final byte[] destiny) {
        final int len = outLength(source, source.length);
        if (destiny.length < len)
            throw new IllegalArgumentException("Output byte array is too small for decoding all input bytes");
        return decode0(source, source.length, destiny);
    }

    private int decode0(final byte[] source, final int sourceLength, final byte[] destiny) {
        int[] base64 = isURL ? FROM_BASE_64_URL : FROM_BASE_64;
        int sourcePosition = 0;
        int destinyPosition = 0;
        int bits = 0;
        int shiftTo = 18;       // pos of first byte of 4-byte atom
        while (sourcePosition < sourceLength) {
            int b = source[sourcePosition++] & 0xFF;
            if ((b = base64[b]) < 0) {
                if (b == -2) {         // padding byte '='
                    // =     shiftTo == 18 unnecessary padding
                    // x=    shiftTo == 12 a dangling single x
                    // x     to be handled together with non-padding case
                    // xx=   shiftTo == 6 && sp == sl missing last =
                    // xx=y  shiftTo == 6 last is not =
                    if (shiftTo == 6 && (sourcePosition == sourceLength || source[sourcePosition++] != '=') || shiftTo == 18) {
                        throw new IllegalArgumentException("Input byte array has wrong 4-byte ending unit");
                    }
                    break;
                }
                if (isMIME)    // skip if for rfc2045
                    continue;
                else
                    throw new IllegalArgumentException("Illegal base64 character " + Integer.toString(source[sourcePosition - 1], 16));
            }
            bits |= (b << shiftTo);
            shiftTo -= 6;
            if (shiftTo < 0) {
                destiny[destinyPosition++] = (byte) (bits >> 16);
                destiny[destinyPosition++] = (byte) (bits >> 8);
                destiny[destinyPosition++] = (byte) (bits);
                shiftTo = 18;
                bits = 0;
            }
        }
        // reached end of byte array or hit padding '=' characters.
        if (shiftTo == 6) {
            destiny[destinyPosition++] = (byte) (bits >> 16);
        } else if (shiftTo == 0) {
            destiny[destinyPosition++] = (byte) (bits >> 16);
            destiny[destinyPosition++] = (byte) (bits >> 8);
        } else if (shiftTo == 12) {
            // dangling single "x", incorrectly encoded.
            throw new IllegalArgumentException("Last unit does not have enough valid bits");
        }
        // anything left is invalid, if is not MIME.
        // if MIME, ignore all non-base64 character
        while (sourcePosition < sourceLength) {
            if (isMIME && base64[source[sourcePosition++]] < 0)
                continue;
            throw new IllegalArgumentException("Input byte array has incorrect ending byte at " + sourcePosition);
        }
        return destinyPosition;
    }
}
