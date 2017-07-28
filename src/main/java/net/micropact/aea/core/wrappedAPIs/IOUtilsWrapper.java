package net.micropact.aea.core.wrappedAPIs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

/**
 * This class wraps functionality from {@link IOUtils}.
 *
 * @author Zachary.Miller
 */
public final class IOUtilsWrapper {

    /**
     * Utility classes do not need public constructors.
     */
    private IOUtilsWrapper(){}

    /**
     * Converts an {@link InputStream} to a byte[].
     *
     * @param inputStream inputStream to convert
     * @return a byte[] of the contents
     * @throws IOException if there was an underlying exception
     */
    public static byte[] toByteArray(final InputStream inputStream) throws IOException{
        return IOUtils.toByteArray(inputStream);
    }

    /**
     * Writes an {@link InputStream} to an {@link OutputStream}.
     *
     * @param inputStream stream to read from
     * @param outputStream stream to write to
     * @throws IOException If there was an underlying {@link IOException}
     */
    public static void copy(final InputStream inputStream, final OutputStream outputStream) throws IOException{
        IOUtils.copy(inputStream, outputStream);
    }
}
