package com.github.jiizuz.base64;

import java.util.Objects;

/**
 * This class consists exclusively of static methods for obtaining
 * encoders and decoders for the Base64 encoding scheme. The
 * implementation of this class supports the following types of Base64
 * as specified in
 * <a href="http://www.ietf.org/rfc/rfc4648.txt">RFC 4648</a> and
 * <a href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045</a>.
 *
 * <ul>
 * <li><a name="basic"><b>Basic</b></a>
 * <p> Uses "The Base64 Alphabet" as specified in Table 1 of
 *     RFC 4648 and RFC 2045 for encoding and decoding operation.
 *     The encoder does not add any line feed (line separator)
 *     character. The decoder rejects data that contains characters
 *     outside the base64 alphabet.</p></li>
 *
 * <li><a name="url"><b>URL and Filename safe</b></a>
 * <p> Uses the "URL and Filename safe Base64 Alphabet" as specified
 *     in Table 2 of RFC 4648 for encoding and decoding. The
 *     encoder does not add any line feed (line separator) character.
 *     The decoder rejects data that contains characters outside the
 *     base64 alphabet.</p></li>
 *
 * <li><a name="mime"><b>MIME</b></a>
 * <p> Uses the "The Base64 Alphabet" as specified in Table 1 of
 *     RFC 2045 for encoding and decoding operation. The encoded output
 *     must be represented in lines of no more than 76 characters each
 *     and uses a carriage return {@code '\r'} followed immediately by
 *     a linefeed {@code '\n'} as the line separator. No line separator
 *     is added to the end of the encoded output. All line separators
 *     or other characters not found in the base64 alphabet table are
 *     ignored in decoding operation.</p></li>
 * </ul>
 *
 * <p> Unless otherwise noted, passing a {@code null} argument to a
 * method of this class will cause a {@link java.lang.NullPointerException
 * NullPointerException} to be thrown.
 *
 * @see Encoder
 * @see Decoder
 * @since 1.0
 */
public class Base64 {

    private Base64() {
        // Un-instantiable class
    }

    /**
     * Returns a {@link Encoder} that encodes using the
     * Basic type base64 encoding scheme.
     *
     * @return A Base64 encoder.
     */
    public static Encoder getEncoder() {
        return Encoder.RFC4648;
    }

    /**
     * Returns a {@link Encoder} that encodes using the
     * URL and Filename safe type base64 encoding scheme.
     *
     * @return A Base64 encoder.
     */
    public static Encoder getUrlEncoder() {
        return Encoder.RFC4648_URL_SAFE;
    }

    /**
     * Returns a {@link Encoder} that encodes using the
     * MIME type base64 encoding scheme.
     *
     * @return A Base64 encoder.
     */
    public static Encoder getMimeEncoder() {
        return Encoder.RFC2045;
    }

    /**
     * Returns a {@link Encoder} that encodes using the
     * MIME type base64 encoding scheme with specified
     * line length and line separators.
     *
     * @param lineLength    the length of each output line (rounded down to nearest multiple
     *                      of 4). If {@code lineLength <= 0} the output will not be separated
     *                      in lines
     * @param lineSeparator the line separator for each output line
     * @return A Base64 encoder.
     * @throws IllegalArgumentException if {@code lineSeparator} includes any
     *                                  character of "The Base64 Alphabet" as specified in Table 1 of
     *                                  RFC 2045.
     */
    public static Encoder getMimeEncoder(final int lineLength, final byte[] lineSeparator) {
        Objects.requireNonNull(lineSeparator);
        final int[] base64 = Decoder.FROM_BASE_64;
        for (final byte b : lineSeparator) {
            if (base64[b & 0xFF] != -1)
                throw new IllegalArgumentException("Illegal base64 line separator character 0x" + Integer.toString(b, 16));
        }
        if (lineLength <= 0) {
            return Encoder.RFC4648;
        }
        return new Encoder(false, lineSeparator, lineLength >> 2 << 2, true);
    }

    /**
     * Returns a {@link Decoder} that decodes using the
     * Basic type base64 encoding scheme.
     *
     * @return A Base64 decoder.
     */
    public static Decoder getDecoder() {
        return Decoder.RFC4648;
    }

    /**
     * Returns a {@link Decoder} that decodes using the
     * URL and Filename safe type base64 encoding scheme.
     *
     * @return A Base64 decoder.
     */
    public static Decoder getUrlDecoder() {
        return Decoder.RFC4648_URL_SAFE;
    }

    /**
     * Returns a {@link Decoder} that decodes using the
     * MIME type base64 decoding scheme.
     *
     * @return A Base64 decoder.
     */
    public static Decoder getMimeDecoder() {
        return Decoder.RFC2045;
    }
}
