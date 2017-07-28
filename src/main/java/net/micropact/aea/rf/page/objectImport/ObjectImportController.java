package net.micropact.aea.rf.page.objectImport;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;

import net.micropact.aea.core.importExport.ComponentDataImporter;
import net.micropact.aea.rf.service.RfImportLogic;
import net.micropact.aea.utility.ImportExportUtility;

/**
 * This page ingests an XML representation of user-entered information regarding the Rules Framework.
 * It ingests files which can be created from the {@link ObjectImportController}
 *
 * @author zmiller
 * @see net.micropact.aea.rf.page.objectExport.ObjectExportController
 * @see ImportExportUtility
 */
public class ObjectImportController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {

        return ComponentDataImporter.performExecute(etk, new RfImportLogic(etk));
    }
}
