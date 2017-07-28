package net.micropact.aea.core.ioUtility;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.entellitrak.ExecutionContext;

import net.micropact.aea.core.wrappedAPIs.IOUtilsWrapper;
import net.micropact.aea.utility.Utility;

/**
 * This class contians utility functionality related to <code>java.io</code>.
 *
 * @author zmiller
 */
public final class IOUtility {

    /**
     * Utility classes do not need constructors.
     * */
    private IOUtility(){}

    /**
     * This method attempts to close an object. If an {@link IOException} is encountered, information about the
     * exception will be written to the log.
     *
     * @param etk entellitrak execution context
     * @param closeable object to be closed
     */
    public static void closeLoggingException(final ExecutionContext etk, final Closeable closeable){
        if(closeable != null){
            try {
                closeable.close();
            } catch (final IOException e) {
                Utility.aeaLog(etk,
                        String.format("Error encountered attempting to close closeable %s", closeable),
                        e);
            }
        }
    }

    /**
     * Closes an object, swallowing any exceptions which occur.
     *
     * @param closeable the object to be closed
     */
    public static void closeQuietly(final Closeable closeable){
        if(closeable != null){
            try{
                closeable.close();
            }catch(final IOException e){
                /* Closing quietly means not throwing a new exception here */
            }
        }
    }

    /**
     * Converts an inputStream to a byte[].
     *
     * @param inputStream inputStream to be converted to a byte array
     * @return a byte[] of the stream's contents
     * @throws IOException if there was an underlying error
     */
    public static byte[] toByteArray(final InputStream inputStream) throws IOException{
        return IOUtilsWrapper.toByteArray(inputStream);
    }

    /**
     * Writes an {@link InputStream} to an {@link OutputStream}.
     *
     * @param inputStream stream to read from
     * @param outputStream stream to write to
     * @throws IOException If there was an underlying {@link IOException}
     */
    public static void copy(final InputStream inputStream, final OutputStream outputStream) throws IOException{
        IOUtilsWrapper.copy(inputStream, outputStream);
    }
}
