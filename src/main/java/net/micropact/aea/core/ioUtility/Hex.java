package net.micropact.aea.core.ioUtility;

import net.micropact.aea.core.wrappedAPIs.HexWrapper;

/**
 * This class contains methods for dealing with hex encoded data.
 *
 * @author Zachary.Miller
 */
public final class Hex {

    /**
     * Utility classes do not need public constructors.
     */
    private Hex(){}

    /**
     * Encode a byte[] as a hex string.
     *
     * @param bytes data to encode
     * @return the encoded data
     */
    public static char[] encodeHex(final byte[] bytes) {
        return HexWrapper.encodeHex(bytes);
    }
}
