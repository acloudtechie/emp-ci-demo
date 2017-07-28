package net.micropact.aea.core.lookup;

import java.util.Map;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;

import net.entellitrak.aea.lookup.IAeaLookupHandler;
import net.micropact.aea.utility.LookupSourceType;
import net.micropact.aea.utility.ScriptObjectLanguageType;
import net.micropact.aea.utility.SystemObjectType;

/**
 * This class contains utility functionality related to lookup metadata.
 *
 * @author zmiller
 */
public final class LookupMetadata {

    /**
     * Hide constructors for utility classes.
     */
    private LookupMetadata(){}

    /**
     * Returns information about the Table and Column that a lookup gets its value from. If it cannot determine the
     * Table and Column, it will return null.
     *
     * @param etk entellitrak executionContext
     * @param lookupDefinitionId if of the lookup definition to get the information about.
     * @return Table and Column that the lookup gets its Value from.
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     * @throws ClassNotFoundException If there was an underlying {@link ClassNotFoundException}
     * @throws IllegalAccessException If there was an underlying {@link IllegalAccessException}
     * @throws InstantiationException If there was an underlying {@link InstantiationException}
     */
    public static TableColumn getLookupReference(final ExecutionContext etk, final long lookupDefinitionId)
            throws IncorrectResultSizeDataAccessException, ClassNotFoundException,
            InstantiationException, IllegalAccessException{

        final LookupSourceType lookupSourceType =
                LookupSourceType.getLookupSourceType(((Number) etk.createSQL("SELECT LOOKUP_SOURCE_TYPE FROM etk_lookup_definition WHERE lookup_definition_id = :lookupDefinitionId")
                        .setParameter("lookupDefinitionId", lookupDefinitionId)
                        .fetchObject()).longValue());

        String tableName = null;
        String columnName = null;

        switch(lookupSourceType){
            case DATA_OBJECT_LOOKUP:
                final Map<String, Object> lookupInfo = etk.createSQL("SELECT dataObject.TABLE_NAME, dataElement.COLUMN_NAME FROM etk_lookup_definition lookupDefinition JOIN etk_data_object dataObject ON dataObject.data_object_id = lookupDefinition.data_object_id LEFT JOIN etk_data_element dataElement ON dataElement.data_element_id = lookupDefinition.value_element_id WHERE lookupDefinition.lookup_definition_id = :lookupDefinitionId")
                .setParameter("lookupDefinitionId", lookupDefinitionId)
                .fetchMap();

                tableName = (String) lookupInfo.get("TABLE_NAME");
                columnName = lookupInfo.get("COLUMN_NAME") == null ? "ID" : (String) lookupInfo.get("COLUMN_NAME");

                break;
            case SCRIPT_LOOKUP:

                final Map<String, Object> scriptObjectInfo = etk.createSQL("SELECT scriptObject.FULLY_QUALIFIED_SCRIPT_NAME, scriptObject.SCRIPT_LANGUAGE_TYPE FROM etk_lookup_definition lookupDefinition JOIN aea_script_pkg_view_sys_only scriptObject ON scriptObject.script_id = lookupDefinition.sql_script_object_id WHERE lookupDefinition.lookup_definition_id = :lookupDefinitionId")
                .setParameter("lookupDefinitionId", lookupDefinitionId)
                .fetchMap();

                if(ScriptObjectLanguageType.JAVA.getId()
                        == ((Number) scriptObjectInfo.get("SCRIPT_LANGUAGE_TYPE")).longValue()){

                    final Object javaObject =
                            Class.forName((String) scriptObjectInfo.get("FULLY_QUALIFIED_SCRIPT_NAME")).newInstance();

                    if(javaObject instanceof IAeaLookupHandler){
                        final IAeaLookupHandler lookupHandler = (IAeaLookupHandler) javaObject;
                        tableName = lookupHandler.getValueTableName(etk);
                        columnName = lookupHandler.getValueColumnName(etk);
                    }
                }
                break;
            case SYSTEM_OBJECT_LOOKUP:
                final SystemObjectType systemObjectType = SystemObjectType.getById(((Number) etk.createSQL("SELECT system_object_type FROM etk_lookup_definition WHERE lookup_definition_id = :lookupDefinitionId")
                        .setParameter("lookupDefinitionId", lookupDefinitionId)
                        .fetchObject()).intValue());
                tableName = systemObjectType.getTableName();
                columnName = systemObjectType.getColumnName();

                break;
            default:
                break;
        }

        if(tableName != null && columnName != null){
            return new TableColumn(tableName, columnName);
        }else{
            return null;
        }
    }

    /**
     * This class represents a database Table and Column combination.
     *
     * @author zmiller
     */
    public static class TableColumn{

        private final String table;
        private final String column;

        /**
         * Constructor for TableColumn.
         *
         * @param tableName name of the table
         * @param columnName name of the column
         */
        TableColumn(final String tableName, final String columnName){
            table = tableName == null ? null : tableName.toUpperCase();
            column = columnName == null ? null : columnName.toUpperCase();
        }

        /**
         * Get the name of the Table.
         *
         * @return name of the table in UPPER case
         */
        public String getTable(){
            return table;
        }

        /**
         * Get the name of the Column.
         *
         * @return name of the column in UPPER case
         */
        public String getColumn(){
            return column;
        }
    }
}
