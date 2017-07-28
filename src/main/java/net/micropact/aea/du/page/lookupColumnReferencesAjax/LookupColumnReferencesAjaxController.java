package net.micropact.aea.du.page.lookupColumnReferencesAjax;

import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.lookup.LookupMetadata;
import net.micropact.aea.core.lookup.LookupMetadata.TableColumn;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.LookupSourceType;
import net.micropact.aea.utility.Utility;

/**
 * This page returns metadata surrounding lookups in JSON format.
 * It is primarily concerned with returning the Table and Column that a lookup refers to.
 *
 * @author zmiller
 */
public class LookupColumnReferencesAjaxController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {

            final TextResponse response = etk.createTextResponse();

            response.setContentType(ContentType.JSON);
            response.put("out",  JsonUtilities.encode(getLookupMetadata(etk)));

            return response;
        } catch (final IncorrectResultSizeDataAccessException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Gets metadata for all of the lookups within this instance of entellitrak.
     *
     * @param etk entellitrak execution context
     * @return Information about lookups in the form
     *  <pre>
     *      [{LOOKUP_DEFINITION_ID: Number,
     *       LOOKUP_SOURCE_TYPE: Number,
     *       NAME: String,
     *       tableName: String,
     *       columnName: String,
     *       tableColumnExists: Boolean,
     *       lookupSourceTypeDisplay: String}]
     *  </pre>
     * @throws IllegalAccessException If there was an underlying {@link IncorrectResultSizeDataAccessException}
     * @throws InstantiationException If there was an underlying {@link InstantiationException}
     * @throws ClassNotFoundException If there was an underlying {@link ClassNotFoundException}
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static List<Map<String, Object>> getLookupMetadata(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException, ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        final List<Map<String, Object>> lookupDefinitions = getLookupDefinitions(etk);

        for(final Map<String, Object> lookupDefinition : lookupDefinitions){
            addTableColumn(etk, lookupDefinition);
        }

        addTableColumnExists(etk, lookupDefinitions);

        for(final Map<String, Object> lookupDefinition : lookupDefinitions){
            addLookupSourceTypeDisplay(lookupDefinition);
        }

        return lookupDefinitions;
    }

    /**
     * This function will add the user-friendly Lookup Source Type display under the key lookupSourceTypeDisplay.
     *
     * @param lookupDefinition The lookup definition to which the display is to be added.
     *      The lookup definition must have a LOOKUP_SOURCE_TYPE with the value of the entellitrak id of the
     *      lookup source type
     */
    private static void addLookupSourceTypeDisplay(final Map<String, Object> lookupDefinition){
        lookupDefinition.put("lookupSourceTypeDisplay",
                LookupSourceType.getLookupSourceType(
                        ((Number) lookupDefinition.get("LOOKUP_SOURCE_TYPE")).longValue()).getDisplay());
    }

    /**
     * This function takes a lookupDefinition with a LOOKUP_DEFINITION_ID key and adds keys for tableName and columnName
     * which will indicate the table and column which the lookup pulls its values from.
     *
     * @param etk entellitrak execution context
     * @param lookupDefinition A Map containing a description of the lookup definition
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     * @throws ClassNotFoundException If there was an underlying {@link ClassNotFoundException}
     * @throws InstantiationException If there was an underlying {@link InstantiationException}
     * @throws IllegalAccessException If there was an underlying {@link IllegalAccessException}
     */
    private static void addTableColumn(final ExecutionContext etk, final Map<String, Object> lookupDefinition)
            throws IncorrectResultSizeDataAccessException, ClassNotFoundException,
            InstantiationException, IllegalAccessException{
        final TableColumn tableColumn =
                LookupMetadata.getLookupReference(etk,
                        ((Number) lookupDefinition.get("LOOKUP_DEFINITION_ID")).longValue());

        lookupDefinition.put("tableName", tableColumn == null ? null : tableColumn.getTable());
        lookupDefinition.put("columnName", tableColumn == null ? null : tableColumn.getColumn());
    }

    /**
     * Returns information about the lookups from the next-to-be-deployed tracking configuration.
     *
     * @param etk entellitrak execution context
     * @return A list of lookups of the form
     *  <pre>
     *      [{LOOKUP_DEFINITION_ID: Number,
     *        LOOKUP_SOURCE_TYPE: Number,
     *        NAME: String}]
     *  </pre>
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static List<Map<String, Object>> getLookupDefinitions(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException{
        return etk.createSQL("SELECT lookupDefinition.LOOKUP_DEFINITION_ID, lookupDefinition.LOOKUP_SOURCE_TYPE, lookupDefinition.NAME, lookupDefinition.business_key LOOKUP_BUSINESS_KEY FROM etk_lookup_definition lookupDefinition WHERE lookupDefinition.tracking_config_id = :trackingConfigId ORDER BY lookupDefinition.name, lookupDefinition.business_key, lookupDefinition.lookup_definition_id")
                .setParameter("trackingConfigId", Utility.getTrackingConfigIdNext(etk))
                .fetchList();
    }

    /**
     * Adds a key tableColumnExists with a boolean value to Maps containing lookup definition information.
     *
     * @param etk entellitrak execution context
     * @param lookupDefinitions List which contains Maps with keys tableName and columnName
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static void addTableColumnExists(final ExecutionContext etk,
            final List<Map<String, Object>> lookupDefinitions)
                    throws IncorrectResultSizeDataAccessException{
        for(final Map<String, Object> lookupDefinition : lookupDefinitions){
            lookupDefinition.put("tableColumnExists",
                    tableColumnExists(etk,
                            (String) lookupDefinition.get("tableName"),
                            (String) lookupDefinition.get("columnName")));
        }
    }

    /**
     * This method checks to see whether a particular table and column exist within the database.
     *
     * @param etk entellitrak execution context
     * @param tableName name of the table
     * @param columnName name of the column
     * @return null if tableName and columnName are null, otherwise true if the table/column exists in the database and
                 false if it does not.
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static Boolean tableColumnExists(final ExecutionContext etk,
            final String tableName,
            final String columnName)
            throws IncorrectResultSizeDataAccessException{

        if(tableName == null || columnName == null){
            return null;
        }else{
            return 1 == etk.createSQL(Utility.isSqlServer(etk) ? "SELECT COUNT(*) FROM information_schema.columns WHERE UPPER(table_name) = :tableName AND UPPER(column_name) = :columnName"
                                                                : "SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE UPPER(table_name) = :tableName AND UPPER(column_name) = :columnName")
                    .setParameter("tableName", tableName)
                    .setParameter("columnName", columnName)
                    .fetchInt();
        }
    }
}
