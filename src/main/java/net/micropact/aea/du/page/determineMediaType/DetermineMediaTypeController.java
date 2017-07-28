package net.micropact.aea.du.page.determineMediaType;

import java.io.IOException;
import java.io.InputStream;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.FileStream;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.ioUtility.MediaTypeUtility;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * This class serves as the controller code for a page which attempts to determine the media types of files in order
 * to be able to whitelist them in the media-type-config.xml configuration file.
 *
 * @author Zachary.Miller
 */
public class DetermineMediaTypeController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try{

            final TextResponse response = etk.createTextResponse();

            final FileStream fileStream = etk.getParameters().getFile("file");

            final String mediaType;

            if(fileStream == null){
                mediaType = null;
            }else{
                mediaType = guessMediaType(fileStream);
            }

            response.put("config", JsonUtilities.encode(Utility.arrayToMap(String.class, Object.class, new Object[][]{
                {"mediaType", mediaType}
            })));

            return response;
        }catch(final IOException e){
            throw new ApplicationException(e);
        }

    }

    /**
     * Attempt to guess the file type which this file should be whitelisted in within the media-type config file.
     *
     * @param fileStream The non-null file stream containing the file to guess the media-type of
     * @return The media-type that this file seems to be
     * @throws IOException If there was an underlying {@link IOException}
     */
    private static String guessMediaType(final FileStream fileStream) throws IOException{
        try (final InputStream inputStream = fileStream.getInputStream()){
            return MediaTypeUtility.guessMediaType(inputStream);
        }
    }
}
