package net.micropact.aea.core.wrappedAPIs;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;

/**
 * Wrapper around {@link FileUtils}.
 *
 * @author Zachary.Miller
 */
public final class FileUtilsWrapper {

    /**
     * Utility classes do not need public constructors.
     */
    private FileUtilsWrapper(){}

    /**
     * Gets a list of all files within a given directory.
     *
     * @param directory directory for list files within
     * @param extensions the file extensions which should be returned
     * @return the the list of matching files
     */
    public static Collection<File> listFiles(final File directory, final String[] extensions) {
        final Collection<File> listFiles = FileUtils.listFiles(directory, extensions, false);
        return listFiles;
    }

    /**
     * Converts a number of bytes into a user-friendly representation of that number of bytes.
     *
     * @param bytes number of bytes
     * @return a user-friendly String
     */
    public static String byteCountToDisplaySize(final long bytes) {
        return FileUtils.byteCountToDisplaySize(bytes);
    }
}
