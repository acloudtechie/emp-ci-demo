package net.micropact.aea.eu.utility;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;

import net.micropact.aea.core.ioUtility.IOUtility;

/**
 * This class contains Utility functions for manipulating things in
 * etk_file,
 * InputStreams,
 * byte[] and
 * other represestations of binary data.
 *
 * TODO: This class actually is not handling streams that great because it is not clear in these methods whether the
 *      caller or the method itself should be responsible for closing various streams. Ideally this entire class
 *      would get refactored. We should seriously consider using byte[] more, or effectively a byte[] backed by a
 *      temporary File, which cleans up its file in its finalize method.
 *      Also, since I now am closing some streams, but some core streams can't be closed twice, check whether
 *      everything still works.
 *
 * @author zmiller
 */
public final class FileUtility {

    /** Constructor is private since all methods are static. */
    private FileUtility(){}

    /**
     * Converts an object representing binary data to an {@link InputStream}.
     * If file is an {@link InputStream}, then this method will close it.
     * <strong>
     *  The caller of this method is responsible for closing the returned {@link InputStream}.
     * </strong>
     *
     * @param etk the context to use if one is needed
     * @param file Object to convert to a file.
     *      It may be any of the following; InputStream, byte[], Long representing a fileId in etk_file
     * @return InputStream representation of the file
     * @throws IncorrectResultSizeDataAccessException If there was an
     * underlying {@link IncorrectResultSizeDataAccessException}
     */
    public static InputStream toInputStream(final ExecutionContext etk, final Object file)
            throws IncorrectResultSizeDataAccessException{
        if(file instanceof InputStream){
            return (InputStream) file;
        }else if(file instanceof byte[]){
            return toInputStream((byte[]) file);
        }else if(file instanceof Long){
            return toInputStream(etk, (Long) file);
        }else{
            throw new IllegalArgumentException(
                    String.format("Object Type not supported for converting object to InputStream: %s",
                            file.getClass().getName()));
        }
    }

    /**
     * This method is for converting an object representing some binary content to a byte[].
     * If its input is an {@link InputStream}, it will be closed by this method.
     *
     * @param etk entellitrak execution context
     * @param file Object to convert to a byte[].
     *          It may be any of the following:
     *          <ul>
     *              <li>{@link InputStream}</li>
     *              <li>byte[]</li>
     *              <li>{@link Long} representing a fileId in etk_file</li>
     *          </ul>
     * @return byte[] representation of file
     * @throws IncorrectResultSizeDataAccessException If there was an
     * underlying {@link IncorrectResultSizeDataAccessException}
     * @throws IOException If there was an underlying {@link IOException}
     */
    public static byte[] toByteArray(final ExecutionContext etk, final Object file)
            throws IOException, IncorrectResultSizeDataAccessException{
        try(final InputStream stream = toInputStream(etk, file)){
            return toByteArray(stream);
        }
    }

    /**
     * This method converts a byte[] to an {@link InputStream}.
     * <strong>
     *  The caller of this method is responlible for closing the {@link InputStream} which gets returned.
     * </strong>
     *
     * @param file object to convert
     * @return InputStream representation of file
     */
    private static InputStream toInputStream(final byte[] file){
        return new ByteArrayInputStream(file);
    }

    /**
     * This method converts an etk_file.file_id to an {@link InputStream}.
     * <strong>
     *  The caller of this method is responsible for closing the {@link InputStream} which gets returned.
     * </strong>
     *
     * @param etk entellitrak execution context
     * @param fileId the ID of a file in ETK_FILE
     * @return InputStream representation of fileId
     * @throws IncorrectResultSizeDataAccessException If there was an
     * underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static InputStream toInputStream(final ExecutionContext etk, final Long fileId)
            throws IncorrectResultSizeDataAccessException {
        //TODO: Update this to work with entellidoc
        return toInputStream(etk, etk.createSQL("SELECT CONTENT FROM etk_file WHERE id = :fileId")
                .setParameter("fileId", fileId)
                .fetchObject());
    }

    /**
     * This method converts an input stream to a byte array.
     *
     * @param file An input stream to convert to a byte array
     *          <strong>This method does not close file</strong>
     * @return byte[] representation of file
     * @throws IOException If there was an underlying {@link IOException}
     */
    private static byte[] toByteArray(final InputStream file) throws IOException{
        return IOUtility.toByteArray(file);
    }
}
