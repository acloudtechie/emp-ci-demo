package net.micropact.aea.du.utility;

import java.util.LinkedList;
import java.util.List;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.configuration.DataElement;
import com.entellitrak.configuration.DataObject;

import net.micropact.aea.core.query.QueryUtility;
import net.micropact.aea.utility.IJson;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * Utility class containing functionality for finding and clearing data elements of type file which reference file ids
 * which no longer exist.
 *
 * @author Zachary.Miller
 */
public final class OrphanedFileCColumnUtility {

    /**
     * Utility classes do not need public constructors.
     */
    private OrphanedFileCColumnUtility(){}

    /**
     * Clean the orphaned "C_" columns which reference non-existent files.
     *
     * @param etk entellitrak execution context
     * @return the cleared records
     * @throws IncorrectResultSizeDataAccessException
     *          If there is an udnerlying {@link IncorrectResultSizeDataAccessException}
     */
    public static List<OrphanedCColumnRecord> cleanOrphanedCColumns(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException {
        final List<OrphanedCColumnRecord> orphanedRecords = findOrphanedCColumns(etk);
        orphanedRecords.forEach(orphanedRecord -> clearOrphanedRecord(etk, orphanedRecord));
        return orphanedRecords;
    }

    /**
     * Find orphaned "C_" columns which reference non-existent files.
     *
     * @param etk entellitrak execution context
     * @return the orphaned records
     * @throws IncorrectResultSizeDataAccessException
     *          If there is an underlying {@link IncorrectResultSizeDataAccessException}
     */
    public static List<OrphanedCColumnRecord> findOrphanedCColumns(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException {
        final List<OrphanedCColumnRecord> orphanedRecords = new LinkedList<>();
        for(final DataElement dataElement : FileUtility.getFileDataElements(etk)){
            findOrphanedFilesForDataElement(etk, dataElement)
            .forEach(trackingId -> orphanedRecords.add(new OrphanedCColumnRecord(dataElement, trackingId)));
        }
        return orphanedRecords;
    }

    /**
     * Find all orphaned tracking ids for records where the specified data element references a non-existent file.
     *
     * @param etk entellitrak execution context
     * @param dataElement the data element
     * @return the tracking ids
     */
    private static List<Long> findOrphanedFilesForDataElement(final ExecutionContext etk, final DataElement dataElement){
        final String tableName = dataElement.getDataObject().getTableName();
        final String columnName = dataElement.getColumnName();
        return QueryUtility.mapsToLongs(etk.createSQL(String.format("SELECT id FROM %s WHERE %s IS NOT NULL AND NOT EXISTS(SELECT * FROM etk_file f WHERE f.id = %s.%s) ORDER BY id",
                tableName,
                columnName,
                tableName,
                columnName))
                .fetchList());
    }

    /**
     * This method updates (to null) all instances of a particular data element's column which are currently storing
     * values of files which cannot be found in etk_file.
     *
     * @param etk entellitrak execution context
     * @param orphanedRecord the orphaned record
     */
    private static void clearOrphanedRecord(final ExecutionContext etk, final OrphanedCColumnRecord orphanedRecord) {
        final DataElement dataElement = orphanedRecord.getDataElement();

        final String tableName = dataElement.getDataObject().getTableName();
        final String columnName = dataElement.getColumnName();
        etk.createSQL(String.format("UPDATE %s SET %s = NULL WHERE id = :trackingId",
                tableName,
                columnName))
        .setParameter("trackingId",  orphanedRecord.getTrackingId())
        .execute();
    }

    /**
     * POJO containing data about columns which are supposed to reference files but the file id doesn't exist.
     *
     * @author Zachary.Miller
     */
    public static class OrphanedCColumnRecord implements IJson{

        private final DataElement dataElement;
        private final long trackingId;

        /**
         * Simple constructor.
         *
         * @param theDataElement the data element
         * @param theTrackingId the tracking id
         */
        public OrphanedCColumnRecord(final DataElement theDataElement, final long theTrackingId) {
            dataElement = theDataElement;
            trackingId = theTrackingId;
        }

        public DataElement getDataElement() {
            return dataElement;
        }

        public Object getTrackingId() {
            return trackingId;
        }

        @Override
        public String encode() {
            final DataObject dataObject = dataElement.getDataObject();
            return JsonUtilities.encode(Utility.arrayToMap(String.class, Object.class, new Object[][]{
                {"dataObjectName", dataObject.getName()},
                {"dataElementName", dataElement.getName()},
                {"dataObjectBusinessKey", dataObject.getBusinessKey()},
                {"trackingId", trackingId},
            }));
        }
    }
}
