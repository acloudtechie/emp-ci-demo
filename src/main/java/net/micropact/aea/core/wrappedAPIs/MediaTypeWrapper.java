package net.micropact.aea.core.wrappedAPIs;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;

/**
 * Wrapper around media type utilities.
 *
 * @author Zachary.Miller
 */
public final class MediaTypeWrapper {

    /**
     * Utility classes do not need public constructors.
     */
    private MediaTypeWrapper(){}

    /**
     * Attempt to guess the file type which this file should be whitelisted in within the media-type config file.
     *
     * @param inputStream The non-null file stream containing the file to guess the media-type of
     * @return The media-type that this file seems to be
     * @throws IOException If there was an underlying {@link IOException}
     */
    public static String guessMediaType(final InputStream inputStream) throws IOException{
        final DefaultDetector detector = new DefaultDetector();
        final TikaInputStream tikaStream = TikaInputStream.get(inputStream);
        // TODO: Eventually the toLowerCase call should be able to be removed. This requires for core to change first.
        return detector.detect(tikaStream, new Metadata()).toString().toLowerCase();
    }
}
