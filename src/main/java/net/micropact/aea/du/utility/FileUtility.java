package net.micropact.aea.du.utility;

import java.util.LinkedList;
import java.util.List;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.configuration.DataElement;

import net.micropact.aea.core.query.QueryUtility;
import net.micropact.aea.utility.DataElementType;
import net.micropact.aea.utility.FileType;
import net.micropact.aea.utility.Utility;

/**
 * Generic entellitrak file utilities.
 *
 * @author zmiller
 */
public final class FileUtility {

    /**
     * There is no reason to create a new instance.
     */
    private FileUtility(){}

    /**
     * Gets a list of all File Data elements within the system.
     *
     * @param etk entellitrak execution context
     * @return The list of data elements
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    public static List<DataElement> getFileDataElements(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException{
        final List<DataElement> fileElements = new LinkedList<>();
        for(final String dataElementKey : QueryUtility.<String>toSimpleList(etk.createSQL("SELECT dataElement.BUSINESS_KEY FROM etk_data_object dataObject JOIN etk_data_element dataElement ON dataElement.data_object_id = dataObject.data_object_id WHERE dataObject.tracking_config_id = :trackingConfigId AND dataElement.data_type = :fileType ORDER BY BUSINESS_KEY")
                .setParameter("trackingConfigId", Utility.getTrackingConfigIdCurrent(etk))
                .setParameter("fileType", DataElementType.FILE.getEntellitrakNumber())
                .fetchList())){
            fileElements.add(etk.getDataElementService().getDataElementByBusinessKey(dataElementKey));
        }
        return fileElements;
    }

    /**
     * Fetches the size of an entellitrak file in bytes.
     *
     * @param etk entellitrak execution context
     * @param fileId file id to get the size of
     * @return the size of the file in bytes
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    public static long getFileSizeBytes(final ExecutionContext etk, final long fileId)
            throws IncorrectResultSizeDataAccessException{
        return ((Number) etk.createSQL("SELECT file_size FROM etk_file WHERE id = :fileId")
                .setParameter("fileId", fileId)
                .fetchObject()).longValue();
    }

    /**
     * Deletes a file.
     *
     * @param etk entellitrak execution context
     * @param fileId the file id
     * @throws IncorrectResultSizeDataAccessException
     *          If there is an underlying {@link IncorrectResultSizeDataAccessException}
     */
    public static void deleteFile(final ExecutionContext etk, final long fileId)
            throws IncorrectResultSizeDataAccessException{
        final FileType fileType = FileType.getByEntellitrakNumber(((Number) etk.createSQL("SELECT FILE_TYPE FROM etk_file WHERE id = :fileId")
                .setParameter("fileId", fileId)
                .fetchObject()).intValue());

        if(fileType == FileType.DOCUMENT_MANAGEMENT_FILE){
            throw new IllegalArgumentException(String.format("Do not know how to delete Document Management files. FileId: \"%s\"",
                    fileId));
        }else{
            deletePlainFile(etk, fileId);
        }
    }

    /**
     * Delete a plain (non-document management) file.
     *
     * @param etk entellitrak execution context
     * @param fileId the file id
     */
    private static void deletePlainFile(final ExecutionContext etk, final long fileId) {
        etk.createSQL("DELETE FROM etk_file WHERE id = :fileId")
        .setParameter("fileId", fileId)
        .execute();
    }
}
