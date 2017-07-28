package net.micropact.aea.core.ioUtility;

import java.io.File;
import java.util.Collection;

import net.micropact.aea.core.wrappedAPIs.FileUtilsWrapper;

/**
 * This class contains utility methods for dealing with files.
 *
 * @author Zachary.Miller
 *
 */
public final class FileUtils {

    /**
     * Utility classes do not need public constructors.
     */
    private FileUtils(){}

    /**
     * Gets a list of all files within a given directory.
     *
     * @param directory directory for list files within
     * @param extensions the file extensions which should be returned
     * @return the the list of matching files
     */
    public static Collection<File> listFiles(final File directory, final String[] extensions) {
        return FileUtilsWrapper.listFiles(directory, extensions);
    }

    /**
     * Converts a number of bytes into a user-friendly representation of that number of bytes.
     *
     * @param bytes number of bytes
     * @return a user-friendly String
     */
    public static String byteCountToDisplaySize(final long bytes) {
        return FileUtilsWrapper.byteCountToDisplaySize(bytes);
    }
}
