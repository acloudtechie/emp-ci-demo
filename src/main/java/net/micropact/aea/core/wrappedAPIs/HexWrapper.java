package net.micropact.aea.core.wrappedAPIs;

import org.apache.commons.codec.binary.Hex;

/**
 * Wrapper around {@link Hex}.
 *
 * @author Zachary.Miller
 */
public final class HexWrapper {

    /**
     * Utility classes do not need public constructors.
     */
    private HexWrapper(){}

    /**
     * Encode a byte[] as a hex string.
     *
     * @param bytes data to encode
     * @return the encoded data
     */
    public static char[] encodeHex(final byte[] bytes) {
        return Hex.encodeHex(bytes);
    }
}
