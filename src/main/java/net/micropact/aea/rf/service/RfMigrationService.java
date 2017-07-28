package net.micropact.aea.rf.service;

import java.io.InputStream;

import com.entellitrak.ExecutionContext;

import net.entellitrak.aea.rf.service.IRfMigrationService;
import net.micropact.aea.utility.ImportExportUtility;

/**
 * Private implementation of the public interface {@link IRfMigrationService}.
 *
 * @author zachary.miller
 */
public class RfMigrationService implements IRfMigrationService {

    private final ExecutionContext etk;

    /**
     * Simple constructor.
     *
     * @param executionContext entellitrak execution context
     */
    public RfMigrationService(final ExecutionContext executionContext) {
        etk = executionContext;
    }

    @Override
    public InputStream exportToStream() throws Exception {
        return ImportExportUtility.getStreamFromDoc(RfExportLogic.generateXml(etk));
    }

    @Override
    public void importFromStream(final InputStream xmlWorkflowStream) throws Exception {
        /* The logic could be moved into RfMigrationService and then RfImportLogic could just call importFromStream*/
        new RfImportLogic(etk).performImport(xmlWorkflowStream);
    }
}
