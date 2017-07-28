package net.micropact.aea.core.ioUtility;

import net.micropact.aea.core.wrappedAPIs.Base64Wrapper;

/**
 * This class contains functionality for dealing with base64 encoded data.
 *
 * @author Zachary.Miller
 */
public final class Base64 {

    /**
     * Utility classes do not need public constructors.
     */
    private Base64(){}

    /**
     * Base64 encodes data.
     *
     * @param content the bytes to encode
     * @return the base64 encoded value
     */
    public static byte[] encodeBase64(final byte[] content){
        return Base64Wrapper.encodeBase64(content);
    }

    /**
     * Base64 decodes data.
     *
     * @param bytes the base64 encoded data
     * @return the decoded data
     */
    public static byte[] decodeBase64(final byte[] bytes) {
        return Base64Wrapper.decodeBase64(bytes);
    }
}
