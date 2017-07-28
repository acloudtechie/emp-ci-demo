/**
 *
 * LogViewerFileList
 *
 * zmiller 06/05/2015
 **/

package net.micropact.aea.du.page.logViewerFileList;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.ioUtility.FileUtils;
import net.micropact.aea.du.utility.LogUtility;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * Page controller code which returns information about the files in the container log directory.
 *
 * @author zmiller
 */
public class LogViewerFileListController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {

        final TextResponse response = etk.createTextResponse();
        response.setContentType(ContentType.JSON);

        response.put("out", JsonUtilities.encode(sendFileList(LogUtility.getLogPath(etk), LogUtility.logExtensions())));

        return response;

    }

    /**
     * Gets data about the files in a directory which have particular file extensions.
     *
     * @param path The path of the directory.
     * @param extensions The file extensions of files which will be returned.
     * @return Map of file information of the form:
     *      <pre>
     *          {files: [{name: String,
     *                    size: Number,
     *                    lastModified: Number}]}
     *      </pre>
     */
    private static Map<String, Object> sendFileList(final String path, final String[] extensions) {
        final File dir = new File(path);

        final Collection<Map<String, Object>> files = new LinkedList<>();

        if (dir.exists() && dir.isDirectory()) {
            final List<File> listFiles = new ArrayList<>(FileUtils.listFiles(dir, extensions));
            Collections.sort(listFiles);

            for (final File file : listFiles) {
                files.add(Utility.arrayToMap(String.class, Object.class, new Object[][]{
                        {"name", file.getName()},
                        {"size", FileUtils.byteCountToDisplaySize(file.length())},
                        {"lastModified", file.lastModified()}}));
            }
        }

        return Utility.arrayToMap(String.class, Object.class, new Object[][]{{
            "files", files
        }});
    }
}
