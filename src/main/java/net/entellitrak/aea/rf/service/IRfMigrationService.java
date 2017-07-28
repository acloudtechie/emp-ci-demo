package net.entellitrak.aea.rf.service;

import java.io.InputStream;

/**
 * This class contains functionality related to migrating Rules Framework configuration data from one environment to
 * another.
 *
 * @author zachary.miller
 */
public interface IRfMigrationService {

    /**
     * This method exports the site-specific Rules Framework configuration data to an input stream in the same format
     * as the RF - Page - Data Export page. This method can be used as part of an automated build script so that the AE
     * does not need to manually run the RF - Page - Data Export page each time a deployment package must be built.
     *
     * @return an input stream containing the site-specific Rules Framework configuration data.
     *          The caller of this method is responsible for closing the stream.
     * @throws Exception If anything went wrong
     *
     * @see #importFromStream(InputStream)
     */
    InputStream exportToStream() throws Exception;

    /**
     * This method imports configuration data in the format produced by RF - Page - Data Export. It can be used as part
     * of a more automated deployment. Examples of how to use it would be to have previously brought the data file over
     * and placed it on the production server, or have brought the file over in an RDO as part of the RDO Export script.
     * Then this method can be invoked by a deployment handler or other page.
     *
     * @param xmlWorkflowStream input stream of data in the format produced by the RF - Page - Data Export page.
     *          This method is not responsible for closing the stream.
     * @throws Exception If anything went wrong
     *
     * @see #exportToStream()
     */
    void importFromStream(InputStream xmlWorkflowStream) throws Exception;
}
