package net.micropact.aea.dbUtils.page.rdodataimportutility;

import java.io.IOException;

import com.entellitrak.ApplicationException;
import com.entellitrak.DataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.FileStream;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.dbUtils.service.RdoImportLogic;

/**
 * RDO import tool that can automatically import both SQL and ETK_FILE content contained
 * within the monolithic rdo_export_timestamp.zip .
 *
 * @author aclee
 */
public class RdoDataImportUtilityController implements PageController {

	private PageExecutionContext etk;
	private StringBuilder sb = new StringBuilder();
	private static final String NEWLINE_CHAR = "<br>";

    @Override
    public Response execute(final PageExecutionContext theEtk)
            throws ApplicationException {

        try {
        	this.etk = theEtk;

            final TextResponse response = etk.createTextResponse();

            final FileStream fileStream = etk.getParameters().getFile("importFile");

            if(fileStream != null){
            	String result = RdoImportLogic.importTwoPartRdoZipFile(theEtk, fileStream.getInputStream());

            	result = result.replaceAll("\n", NEWLINE_CHAR);
            	result = result.replaceAll("Error processing file",
            			                   "<strong style=\"color: red;\">Error processing file</strong>");
            	result = result.replaceAll("Error importing ETK_FILE data",
		                   "<strong style=\"color: red;\">Error importing ETK_FILE data</strong>");
            	result = result.replaceAll("RDO data import failed, rolling back changes",
		                   "<strong style=\"color: red;\">RDO data import failed, rolling back changes</strong>");

            	sb.append(result);

                response.put("result", sb.toString());
            }

            return response;
        } catch (final IOException e) {
            throw new ApplicationException(e);
        } catch (DataAccessException e) {
        	 throw new ApplicationException(e);
		}
    }
}