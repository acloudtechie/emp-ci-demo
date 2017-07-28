package net.micropact.aea.du.page.cleanOrphanedFiles;

import com.entellitrak.ApplicationException;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.du.utility.FileUtility;
import net.micropact.aea.du.utility.OrphanedEtkFileUtility;
import net.micropact.aea.du.utility.OrphanedFileCColumnUtility;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * Controller code for a page which deletes files from etk_file which are not referenced anymore.
 *
 * @see FileUtility
 * @author zmiller
 */
public class CleanOrphanedFilesController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();

            response.put("fileSummary", JsonUtilities.encode(
                    Utility.arrayToMap(String.class, Object.class, new Object[][]{
                        {"orphanedFiles", OrphanedEtkFileUtility.cleanOrphanedFiles(etk)},
                        {"orphanedRecords", OrphanedFileCColumnUtility.cleanOrphanedCColumns(etk)}
                    })));

            return response;
        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }
}
