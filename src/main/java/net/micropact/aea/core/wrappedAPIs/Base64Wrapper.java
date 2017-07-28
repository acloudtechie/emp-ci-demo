package net.micropact.aea.core.wrappedAPIs;

import org.apache.commons.codec.binary.Base64;

/**
 * Wrapper around {@link Base64}.
 *
 * @author Zachary.Miller
 */
public final class Base64Wrapper {

    /**
     * Utility classes do not need public constructors.
     */
    private Base64Wrapper(){}

    /**
     * Base64 encodes data.
     *
     * @param content the bytes to encode
     * @return the base64 encoded value
     */
    public static byte[] encodeBase64(final byte[] content){
        return Base64.encodeBase64(content);
    }

    /**
     * Base64 decodes data.
     *
     * @param bytes the base64 encoded data
     * @return the decoded data
     */
    public static byte[] decodeBase64(final byte[] bytes) {
        return Base64.decodeBase64(bytes);
    }
}
