package net.micropact.aea.core.ioUtility;

import java.io.IOException;
import java.io.InputStream;

import net.micropact.aea.core.wrappedAPIs.MediaTypeWrapper;

/**
 * This class contains useful functionality for dealing with media types.
 *
 * @author Zachary.Miller
 */
public final class MediaTypeUtility {

    /**
     * Utility classes do not need public constructors.
     */
    private MediaTypeUtility(){}

    /**
     * Attempt to guess the file type which this file should be whitelisted in within the media-type config file.
     *
     * @param inputStream The non-null file stream containing the file to guess the media-type of
     * @return The media-type that this file seems to be
     * @throws IOException If there was an underlying {@link IOException}
     */
    public static String guessMediaType(final InputStream inputStream) throws IOException{
        return MediaTypeWrapper.guessMediaType(inputStream);
    }
}
