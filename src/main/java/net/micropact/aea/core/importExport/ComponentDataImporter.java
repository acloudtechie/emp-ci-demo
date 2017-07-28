package net.micropact.aea.core.importExport;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.FileStream;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.ioUtility.IOUtility;
import net.micropact.aea.utility.JsonUtilities;

/**
 * This class is intended to capture much of the shared logic among the Component Data Import pages.
 *
 * @author zmiller
 */
public final class ComponentDataImporter {

    /**
     * Utility classes do not need constructors.
     */
    private ComponentDataImporter(){}

    /**
     * This method does the job of extracting parameters from a {@link PageExecutionContext} and calling an object
     * which is capable of doing the actual import with the file data. The page controller should have parameters
     * <code>update</code> and a file named <code>importFile</code>.
     * The text response it returns contains keys <code>errors</code> and <code>importCompleted</code>.
     *
     * @param etk entellitrak execution context
     * @param importLogic An object which is capable of actually ingesting the file contents and updating the database
     *          with the correct values.
     * @return The response that the page should return.
     * @throws ApplicationException If there was an underlying {@link Exception}
     */
    public static TextResponse performExecute(final PageExecutionContext etk,
            final IImportLogic importLogic) throws ApplicationException{

        InputStream fileStream = null;

        try {
            final boolean update = "1".equals(etk.getParameters().getSingle("update"));
            Boolean importCompleted = false;

            final TextResponse response = etk.createTextResponse();
            final List<String> errors = new LinkedList<>();

            if(update){
                final FileStream fileParameter = etk.getParameters().getFile("importFile");

                if(fileParameter == null){
                    errors.add("You must upload a file");
                }else{
                    fileStream = fileParameter.getInputStream();
                    importLogic.performImport(fileStream);
                    importCompleted = true;
                }
            }

            response.put("errors", JsonUtilities.encode(errors));
            response.put("importCompleted", JsonUtilities.encode(importCompleted));

            return response;
        }finally{
            IOUtility.closeQuietly(fileStream);
        }

    }

}
