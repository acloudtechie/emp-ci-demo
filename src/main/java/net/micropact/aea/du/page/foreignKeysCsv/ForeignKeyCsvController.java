package net.micropact.aea.du.page.foreignKeysCsv;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.FileResponse;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;

import net.micropact.aea.core.data.CsvTools;
import net.micropact.aea.core.lookup.LookupMetadata;
import net.micropact.aea.core.lookup.LookupMetadata.TableColumn;
import net.micropact.aea.utility.IJson;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * This class contains the controller code for a page which will generate a CSV file containing all the foreign keys
 * for tables defined within the application (ie: it does not include ETK_ or JPBM_ tables). For columns that it
 * knows are foreign keys, but does not know what they point to, the CSV will contain them, but have blank entries.
 *
 * @author zmiller
 */
public class ForeignKeyCsvController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
            final FileResponse response = etk.createFileResponse("foreign_keys.csv",
                    encodeCsv(getAllForeignKeys(etk))
                    .getBytes());

            response.setContentType("text/csv");

            return response;

        } catch (final IncorrectResultSizeDataAccessException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Compares two Strings, either of which may be null. nulls are less than any other String.
     *
     * @param s1 first string
     * @param s2 second string
     * @return a negative number if s1 &lt; s2, zero if s1 equals s2, a positive number if s1 &gt; s2
     */
    static int compareNullSafe(final String s1, final String s2){
        if(s1 == null && s2 == null){
            return 0;
        }else if(s1 == null){
            return -1;
        }else if(s2 == null){
            return 1;
        }else{
            return s1.compareTo(s2);
        }
    }

    /**
     * Encodes key representations into a valid CSV String.
     *
     * @param keyRepresentations a list of key representations to be converted to CSV
     * @return a String representing a CSV.
     */
    private static String encodeCsv(final List<KeyRepresentation> keyRepresentations){

        sort(keyRepresentations);

        final StringBuilder builder = new StringBuilder();

        builder.append("*Child Table*,*Child Column*,*Parent Table*,*Parent Column*,*Add Foreign Key (These keys should be dropped immediately after creation of the ERD)*,*Drop Foreign Key (These will NOT match if you have added lookups since creating the keys)*\n");

        long foreignKeyCurrentIndex = 0;

        for(final KeyRepresentation keyRepresentation : keyRepresentations){
            if(keyRepresentation.hasReference()){
                foreignKeyCurrentIndex += 1;
            }

            builder.append(encodeCsv(keyRepresentation, foreignKeyCurrentIndex));
        }

        return builder.toString();
    }

    /**
     * Encodes a key representation as a row for a CSV file.
     *
     * @param keyRepresentation a key representation to be converted to a CSV row.
     * @param foreignKeyCurrentIndex The number of this foreign key in our CSV file
     * @return A row (with trailing newline) for a CSV file.
     */
    private static String encodeCsv(final KeyRepresentation keyRepresentation, final long foreignKeyCurrentIndex){

        return String.format("%s,%s,%s,%s,%s,%s\n", CsvTools.encodeCsv(keyRepresentation.getChildTable()),
                CsvTools.encodeCsv(keyRepresentation.getChildColumn()),
                CsvTools.encodeCsv(keyRepresentation.getParentTable()),
                CsvTools.encodeCsv(keyRepresentation.getParentColumn()),
                CsvTools.encodeCsv(generateCreateForeignKeyStatement(keyRepresentation, foreignKeyCurrentIndex)),
                CsvTools.encodeCsv(dropForeignKey(keyRepresentation, foreignKeyCurrentIndex)));
    }

    /**
     * Generates the name to use for a particular Foreign Key.
     *
     * @param foreignKeyNumber The number of the key as it appears in the CSV
     * @return The name of the index to use for the foreign key
     */
    private static String generateForeignKeyName(final long foreignKeyNumber){
        return String.format("ETK_FK_%s", foreignKeyNumber);
    }

    /**
     * Generates a SQL statement to add a foreign key constraint.
     *
     * @param keyRepresentation The information regarding the key which is to be created
     * @param foreignKeyNumber The number of the key to be created.
     * @return An SQL statement to add the foreign key constraint
     */
    private static String generateCreateForeignKeyStatement (final KeyRepresentation keyRepresentation,
            final long foreignKeyNumber) {
        return keyRepresentation.hasReference()
                ? String.format("ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s(%s);",
                        keyRepresentation.getChildTable(),
                        generateForeignKeyName(foreignKeyNumber),
                        keyRepresentation.getChildColumn(),
                        keyRepresentation.getParentTable(),
                        keyRepresentation.getParentColumn())
                : null;
    }

    /**
     * Generates a SQL statement to drop the foreign key.
     *
     * @param keyRepresentation The data describing the key to be dropped
     * @param foreignKeyNumber The number of this key in our CSV file
     * @return An SQL statement to drop the foreign key constraint
     */
    private static String dropForeignKey (final KeyRepresentation keyRepresentation, final long foreignKeyNumber) {
        return keyRepresentation.hasReference()
                ? String.format("ALTER TABLE %s DROP CONSTRAINT %s;",
                keyRepresentation.getChildTable(),
                generateForeignKeyName(foreignKeyNumber))
                : null;
    }

    /**
     * Sorts a list of key representations in order to be more user-friendly.
     *
     * @param keyRepresentations key representations to be sorted
     */
    private static void sort(final List<KeyRepresentation> keyRepresentations){
        Collections.sort(keyRepresentations, (o1, o2) -> compareNullSafe(o1.getParentColumn(), o2.getParentColumn()));
        Collections.sort(keyRepresentations, (o1, o2) -> compareNullSafe(o1.getParentTable(), o2.getParentTable()));
        Collections.sort(keyRepresentations, (o1, o2) -> compareNullSafe(o1.getChildColumn(), o2.getChildColumn()));
        Collections.sort(keyRepresentations, (o1, o2) -> compareNullSafe(o1.getChildTable(), o2.getChildTable()));
        Collections.sort(keyRepresentations, (o1, o2) -> (o1.getParentColumn() == null ? "a" : "b")
                .compareTo(o2.getParentColumn() == null ? "a" : "b"));
    }

    /**
     * Gets information regarding all foreign keys in user-configurable code within entellitrak.
     * This includes:
     *  <ul>
     *      <li>ID_PARENT</li>
     *      <li>ID_BASE</li>
     *      <li>Lookups</li>
     *      <li>Multiselects</li>
     *  </ul>
     *
     * @param etk entellitrak execution context
     * @return A list of key representations.
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     * @throws ClassNotFoundException If there was an underlying {@link ClassNotFoundException}
     * @throws InstantiationException If there was an underlying {@link InstantiationException}
     * @throws IllegalAccessException If there was an underlying {@link IllegalAccessException}
     */
    private static List<KeyRepresentation> getAllForeignKeys(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException, ClassNotFoundException,
            InstantiationException, IllegalAccessException{
        final List<KeyRepresentation> returnList = new LinkedList<>();

        returnList.addAll(getParentIds(etk));
        returnList.addAll(getBaseIds(etk));
        returnList.addAll(getLookupsSingle(etk));
        returnList.addAll(getLookupsMulti(etk));

        return returnList;
    }

    /**
     * Gets the foreign keys arising from entellitrak multiselects. Each multiselect creates two foreign keys.
     * One is the id_owner of the m_ table, the other is the final column on the m_ table.
     *
     * @param etk entellitrak execution context
     * @return The list of key representations.
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     * @throws ClassNotFoundException If there was an underlying {@link ClassNotFoundException}
     * @throws InstantiationException If there was an underlying {@link InstantiationException}
     * @throws IllegalAccessException If there was an underlying {@link IllegalAccessException}
     */
    private static List<KeyRepresentation> getLookupsMulti(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException, ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        final List<KeyRepresentation> returnList = new LinkedList<>();

        for(final Map<String, Object> dataElement : etk.createSQL("SELECT dataObject.table_name DATA_OBJECT_TABLE_NAME, dataElement.column_name DATA_ELEMENT_COLUMN, dataElement.LOOKUP_DEFINITION_ID, dataElement.table_name M_TABLE_NAME FROM etk_data_object dataObject JOIN etk_data_element dataElement ON dataObject.data_object_id = dataElement.data_object_id WHERE dataObject.tracking_config_id = :trackingConfigId AND dataElement.bound_to_lookup = 1 AND dataElement.table_name IS NOT NULL")
                .setParameter("trackingConfigId", Utility.getTrackingConfigIdNext(etk))
                .fetchList()){

            final String childTable = (String) dataElement.get("M_TABLE_NAME");
            final TableColumn tableColumn =
                    LookupMetadata.getLookupReference(etk,
                            ((Number) dataElement.get("LOOKUP_DEFINITION_ID")).longValue());

            returnList.add(new KeyRepresentation((String) dataElement.get("DATA_OBJECT_TABLE_NAME"),
                    "ID",
                    childTable,
                    "ID_OWNER"));

            returnList.add(new KeyRepresentation(tableColumn == null
                        ? null
                        : (String) tableColumn.getTable(),
                    tableColumn == null ? null : (String) tableColumn.getColumn(),
                    childTable,
                    (String) dataElement.get("DATA_ELEMENT_COLUMN")));
        }

        return returnList;
    }

    /**
     * Gets the foreign keys in entellitrak arising from single selects within the system.
     *
     * @param etk entellitrak execution context
     * @return The list of key representations.
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     * @throws ClassNotFoundException If there was an underlying {@link ClassNotFoundException}
     * @throws InstantiationException If there was an underlying {@link InstantiationException}
     * @throws IllegalAccessException If there was an underlying {@link IllegalAccessException}
     */
    private static List<KeyRepresentation> getLookupsSingle(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException, ClassNotFoundException,
            InstantiationException, IllegalAccessException{
        final List<KeyRepresentation> returnList = new LinkedList<>();

        for(final Map<String, Object> dataElement : etk.createSQL("SELECT dataObject.TABLE_NAME, dataElement.COLUMN_NAME, dataElement.LOOKUP_DEFINITION_ID FROM etk_data_object dataObject JOIN etk_data_element dataElement ON dataObject.data_object_id = dataElement.data_object_id WHERE dataObject.tracking_config_id = :trackingConfigId AND dataElement.bound_to_lookup = 1 AND dataElement.table_name IS NULL")
                .setParameter("trackingConfigId", Utility.getTrackingConfigIdNext(etk))
                .fetchList()){

            final TableColumn tableColumn =
                    LookupMetadata.getLookupReference(etk,
                            ((Number) dataElement.get("LOOKUP_DEFINITION_ID")).longValue());

            returnList.add(new KeyRepresentation(tableColumn == null ? null : tableColumn.getTable(),
                                                 tableColumn == null ? null : tableColumn.getColumn(),
                                                 (String) dataElement.get("TABLE_NAME"),
                                                 (String) dataElement.get("COLUMN_NAME")));

        }

        return returnList;
    }

    /**
     * Returns a list containing information regarding all foreign keys within entellitrak arising from ID_BASE of CTOs.
     *
     * @param etk entellitrak execution context
     * @return The list of key representations
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static List<KeyRepresentation> getBaseIds(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException{
        final List<KeyRepresentation> returnList = new LinkedList<>();

        for(final Map<String, Object> childObject : etk.createSQL("SELECT childObject.PARENT_OBJECT_ID, childObject.TABLE_NAME FROM etk_data_object childObject WHERE childObject.tracking_config_id = :trackingConfigId AND childObject.parent_object_id IS NOT NULL")
                .setParameter("trackingConfigId", Utility.getTrackingConfigIdNext(etk))
                .fetchList()){
            returnList.add(new KeyRepresentation(
                    getBaseTableName(etk, ((Number) childObject.get("PARENT_OBJECT_ID")).longValue()),
                    "ID",
                    (String) childObject.get("TABLE_NAME"),
                    "ID_BASE"));
        }

        return returnList;
    }

    /**
     * Returns a list containing information regarding all foreign keys within entellitrak arising from the ID_PARENT
     * of CTOs.
     *
     * @param etk entellitrak execution context
     * @return The list of foreign keys
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static List<KeyRepresentation> getParentIds(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException{
        final List<KeyRepresentation> returnList = new LinkedList<>();

        for(final Map<String, Object> childObject : etk.createSQL("SELECT childObject.table_name CHILD_TABLE, parentObject.table_name PARENT_TABLE FROM etk_data_object childObject JOIN etk_data_object parentObject ON parentObject.data_object_id = childObject.parent_object_id WHERE childObject.tracking_config_id = :trackingConfigId")
                .setParameter("trackingConfigId", Utility.getTrackingConfigIdNext(etk))
                .fetchList()){
            returnList.add(new KeyRepresentation((String) childObject.get("PARENT_TABLE"),
                    "ID",
                    (String) childObject.get("CHILD_TABLE"),
                    "ID_PARENT"));
        }

        return returnList;
    }

    /**
     * Gets the name of the table of the BTO for an entellitrak data object.
     * If you pass in an RDO or BTO, you will get the name of that object itself.
     *
     * @param etk entellitrak execution context
     * @param dataObjectId internal entellitrak id of the data object definition.
     * @return The name of the Base Table
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static String getBaseTableName(final ExecutionContext etk, final long dataObjectId)
            throws IncorrectResultSizeDataAccessException {

        Map<String, Object> dataObjectInfo = null;
        Long parentId = dataObjectId;

        do {
            dataObjectInfo = etk.createSQL("SELECT childObject.PARENT_OBJECT_ID, childObject.TABLE_NAME FROM etk_data_object childObject WHERE childObject.data_object_id = :dataObjectId")
                    .setParameter("dataObjectId", parentId)
                    .fetchMap();
            parentId = dataObjectInfo.get("PARENT_OBJECT_ID") == null
                    ? null
                    : ((Number) dataObjectInfo.get("PARENT_OBJECT_ID")).longValue();
        } while (parentId != null);

        return (String) dataObjectInfo.get("TABLE_NAME");
    }

    /**
     * This class represents a foreign key relationship.
     * It contains information on the table and column for each side of the key.
     * The "child" is the column which actually has the constraint. The "parent" is the column
     * which the child points to.
     *
     * @author zmiller
     */
    private static final class KeyRepresentation implements IJson{
        private final String theParentTable;
        private final String theParentColumn;
        private final String theChildTable;
        private final String theChildColumn;

        /**
         * Constructs a new KeyRepresentation (child points to parent).
         *
         * @param parentTable The table of the parent
         * @param parentColumn The column of the parent
         * @param childTable The table of the child
         * @param childColumn The column of the child
         */
        KeyRepresentation(final String parentTable,
                final String parentColumn,
                final String childTable,
                final String childColumn){
            theParentTable  = (parentTable  == null)  ? parentTable  : parentTable.toUpperCase();
            theParentColumn = (parentColumn == null)  ? parentColumn : parentColumn.toUpperCase();
            theChildTable   = (childTable   == null)  ? childTable   : childTable.toUpperCase();
            theChildColumn  = (childColumn  == null)  ? childColumn  : childColumn.toUpperCase();
        }

        /**
         * Get the parent table.
         *
         * @return The table of the parent of the key
         */
        public String getParentTable(){
            return theParentTable;
        }

        /**
         * Get the parent column.
         *
         * @return The column of the parent of the key
         */
        public String getParentColumn(){
            return theParentColumn;
        }

        /**
         * Get the child table.
         *
         * @return The table of the child of the key
         */
        public String getChildTable(){
            return theChildTable;
        }

        /**
         * Get the child column.
         *
         * @return The column of the child of the key
         */
        public String getChildColumn(){
            return theChildColumn;
        }

        /**
         * TODO: This should attempt to do better when it comes to eliminating Views or non-unique columns.
         *
         * @return Whether they key actually has a reference to a table/column combination or not
         */
        public boolean hasReference(){
            return theParentTable != null && theChildTable != null;
        }

        @Override
        public String encode() {
            return JsonUtilities.encode(Utility.arrayToMap(String.class, String.class, new String[][]{
                {"parentTable", theParentTable},
                {"parentColumn", theParentColumn},
                {"childTable", theChildTable},
                {"childColumn", theChildColumn}}));
        }
    }
}
