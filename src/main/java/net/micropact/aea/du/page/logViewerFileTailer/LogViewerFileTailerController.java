/**
 *
 * LogViewerTailerController
 *
 * zmiller 06/05/2015
 **/

package net.micropact.aea.du.page.logViewerFileTailer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.legacy.util.StringUtility;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.du.utility.LogUtility;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * Controller code for a page which will get information about the tail of a file in the container log directory.
 * It expects to be passed the fileName and position within the file.
 * It will return the position, filePosition, and lines between the position and finalPosition.
 * If the starting position is null, it will begin and the END of the file and return no lines (however this is
 * not useless information because the caller now knows where the end of the file is for future requests).
 *
 * @author zmiller
 */
public class LogViewerFileTailerController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();
            response.setContentType(ContentType.JSON);

            final String positionString = etk.getParameters().getSingle("position");
            final String fileName = etk.getParameters().getSingle("fileName");

            final Long position = StringUtility.isBlank(positionString) ?  null : Long.parseLong(positionString);

            response.put("out", JsonUtilities.encode(
                    getTail(new File(LogUtility.getLogPath(etk), fileName), position)));

            return response;

        } catch (final IOException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Gets information about the tail of a file.
     *
     * @param file The file to get the tail of
     * @param beginPosition The position to begin tailing at. If it is null, it will begin tailing at the end
     *          (which will return no lines)
     * @return A Map of information of the form:
     *      <pre>
     *          {   startingPosition: Number,
     *              finalPosition: Number,
     *              lines: [String]}
     *      </pre>
     * @throws IOException
     *      If there was an underlying IOException
     */
    private static Map<String, Object> getTail(final File file, final Long beginPosition) throws IOException{
        try(final RandomAccessFile reader = new RandomAccessFile(file, "r")){
            final long length = reader.length();
            final long startingPosition = beginPosition == null ? length : beginPosition;
            reader.seek(startingPosition);

            final List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            final long finalPosition = reader.getFilePointer();

            return Utility.arrayToMap(String.class, Object.class, new Object[][]{
                {"startingPosition", startingPosition},
                {"finalPosition", finalPosition},
                {"lines", lines}});
        }
    }
}
