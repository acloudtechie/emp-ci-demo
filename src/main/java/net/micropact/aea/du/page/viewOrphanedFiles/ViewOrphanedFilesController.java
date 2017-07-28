package net.micropact.aea.du.page.viewOrphanedFiles;

import com.entellitrak.ApplicationException;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.du.utility.OrphanedEtkFileUtility;
import net.micropact.aea.du.utility.OrphanedFileCColumnUtility;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * Controller code for a page which allows viewing orphaned files.
 *
 * @author Zachary.Miller
 */
public class ViewOrphanedFilesController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();

            response.put("fileSummary", JsonUtilities.encode(
                    Utility.arrayToMap(String.class, Object.class, new Object[][]{
                        {"orphanedFiles", OrphanedEtkFileUtility.findOrphanedFiles(etk)},
                        {"orphanedRecords", OrphanedFileCColumnUtility.findOrphanedCColumns(etk)}
                    })));

            return response;
        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }
}
