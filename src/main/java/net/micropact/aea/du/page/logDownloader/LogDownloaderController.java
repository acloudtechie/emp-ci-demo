package net.micropact.aea.du.page.logDownloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.FileResponse;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;

import net.micropact.aea.du.utility.LogUtility;

/**
 * Controller code for a page which downloads a file from the container's log directory.
 *
 * @author zmiller
 */
public class LogDownloaderController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
            final String fileName = etk.getParameters().getSingle("fileName");

            final File f = new File(LogUtility.getLogPath(etk), fileName);

            FileResponse fileResponse;
            fileResponse = etk.createFileResponse(fileName, new FileInputStream(f));

            fileResponse.setContentType(ContentType.OCTET_STREAM);
            fileResponse.setHeader("Content-disposition",
                    String.format("attachment;filename=\"%s.txt\"", fileName));

            return fileResponse;
        } catch (final FileNotFoundException e) {
            throw new ApplicationException(e);
        }
    }
}
