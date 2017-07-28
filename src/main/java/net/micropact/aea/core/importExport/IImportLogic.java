package net.micropact.aea.core.importExport;

import java.io.InputStream;

import com.entellitrak.ApplicationException;

/**
 * This interface represents an object which takes a file containing import data and then updates the database
 * with the appropriate data.
 *
 * @see ComponentDataImporter
 * @author zmiller
 */
public interface IImportLogic {

    /**
     * Takes an input file and updates the database with the data in the file.
     *
     * @param inputStream The data to be imported
     * @throws ApplicationException If there was an underlying {@link Exception}
     */
    void performImport(InputStream inputStream) throws ApplicationException;
}
