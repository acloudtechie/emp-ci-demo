package net.micropact.aea.du.utility;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;

import net.micropact.aea.core.query.QueryUtility;
import net.micropact.aea.utility.FileType;

/**
 * Contains utility functionality for dealing with orphaned etk_file records.
 * It can provide a summary of orphaned files as well as delete those files.
 * TODO: When core fixes etk.getDocumentManagementServiceFactory().getFileService().delete(fileId) this file needs to be
 * updated to delete Document Management files.
 *
 * @author Zachary.Miller
 */
public final class OrphanedEtkFileUtility {

    /**
     * Utility classes do not need public constructors.
     */
    private OrphanedEtkFileUtility(){}

    /**
     * Clean the orphaned files in the etk_file table.
     *
     * @param etk entellitrak execution context
     * @return a summary of the cleaned files
     * @throws IncorrectResultSizeDataAccessException
     *      If there is an underlying {@link IncorrectResultSizeDataAccessException}
     */
    public static List<Long> cleanOrphanedFiles(final ExecutionContext etk) throws IncorrectResultSizeDataAccessException {
        final List<Long> orphanedFiles = findOrphanedFiles(etk);

        orphanedFiles.forEach(fileId -> {
            try {
                FileUtility.deleteFile(etk, fileId);
            } catch (final IncorrectResultSizeDataAccessException e) {
                throw new RuntimeException(e);
            }
        });

        return orphanedFiles;
    }

    /**
     * Finds orphaned files within entellitrak.
     *
     * @param etk entellitrak execution context
     * @return the list of orphaned file ids
     * @throws IncorrectResultSizeDataAccessException
     *          If there is an underlying {@link IncorrectResultSizeDataAccessException}
     */
    public static List<Long> findOrphanedFiles(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException{

        final List<Long> orphanedFileIds = new LinkedList<>();

        // Get a list of all objectTypes (tableNames) in etk_file.
        final List<Map<String, Object>> objectTypeInfos = etk.createSQL("SELECT DISTINCT OBJECT_TYPE FROM etk_file ORDER BY object_type")
                .fetchList();

        for(final Map<String, Object> objectTypeInfo : objectTypeInfos){
            final String objectType = (String) objectTypeInfo.get("OBJECT_TYPE");

            //Check if the table actually exists in entellitrak
            if(0 < etk.createSQL("SELECT COUNT(*) FROM etk_data_object dataObject WHERE dataObject.tracking_config_id = (SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive) AND table_name = :tableName")
                    .setParameter("tableName", objectType)
                    .fetchInt()){
                // The table exists so delete all files which are in etk_file but do not have a matching reference id
                orphanedFileIds.addAll(QueryUtility.mapsToLongs(etk.createSQL("SELECT f.id FROM etk_file f WHERE f.object_type = :objectType AND f.file_type NOT IN(:documentManagementFileTypes) AND NOT EXISTS (SELECT * FROM " + objectType + " obj WHERE obj.id = f.reference_id) ORDER BY f.id")
                        .setParameter("objectType", objectType)
                        .setParameter("documentManagementFileTypes",
                                Arrays.asList(new Integer[]{FileType.DOCUMENT_MANAGEMENT_FILE.getEntellitrakNumber()}))
                        .fetchList()));
            }else{
                //The table does not exist so delete all files with that object type
                orphanedFileIds.addAll(QueryUtility.mapsToLongs(etk.createSQL("SELECT f.id FROM etk_file f WHERE f.object_type = :objectType AND f.file_type NOT IN(:documentManagementFileTypes) ORDER BY f.id")
                        .setParameter("objectType", objectType)
                        .setParameter("documentManagementFileTypes",
                                Arrays.asList(new Integer[]{FileType.DOCUMENT_MANAGEMENT_FILE.getEntellitrakNumber()}))
                        .fetchList()));
            }
        }
        return orphanedFileIds;
    }
}
