package net.micropact.aea.du.page.unusedColumns;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * This page attempts to find all columns which exist in the database
 * but do not appear to be being used by entellitrak.
 * This is a common occurrence when a data element is deleted through the front-end because entellitrak will not
 * automatically delete the database column.
 * @author zmiller
 */
public class UnusedColumnsController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {
        final TextResponse response = etk.createTextResponse();

        final List<Map<String, Object>> unusedColumns = new LinkedList<>();

        /* Multiselect table columns */
        unusedColumns.addAll(etk.createSQL(
                Utility.isSqlServer(etk) ? "SELECT dataElement.TABLE_NAME, columns.COLUMN_NAME FROM etk_data_object dataObject JOIN etk_data_element dataElement ON dataElement.data_object_id = dataObject.data_object_id JOIN information_schema.columns columns ON columns.table_name = dataElement.table_name WHERE dataObject.tracking_config_id = (SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive) AND ISNULL(dataElement.table_name, '') != '' AND columns.column_name NOT IN('ID', 'ID_OWNER', 'LIST_ORDER', dataElement.column_name) ORDER BY dataElement.TABLE_NAME, columns.COLUMN_NAME"
                                           : "SELECT dataElement.TABLE_NAME, columns.COLUMN_NAME FROM etk_data_object dataObject JOIN etk_data_element dataElement ON dataElement.data_object_id = dataObject.data_object_id JOIN user_tab_cols columns ON columns.table_name = dataElement.table_name WHERE dataObject.tracking_config_id = (SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive ) AND dataElement.table_name IS NOT NULL AND columns.column_name NOT IN('ID', 'ID_OWNER', 'LIST_ORDER', dataElement.column_name) ORDER BY dataElement.TABLE_NAME, columns.COLUMN_NAME")
                .fetchList());

        /* Regular table columns */
        unusedColumns.addAll(etk.createSQL(
                Utility.isSqlServer(etk) ? "SELECT dataObject.TABLE_NAME, columns.COLUMN_NAME FROM etk_data_object dataObject JOIN information_schema.columns columns ON columns.table_name = dataObject.table_name WHERE dataObject.tracking_config_id = (SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive) AND columns.column_name NOT IN('ID') AND NOT (dataObject.object_type = 1 AND dataObject.base_object = 0 AND columns.column_name IN ('ID_BASE', 'ID_PARENT')) AND NOT (dataObject.base_object = 1 AND column_name IN('ID_HIERARCHY', 'ID_ARCHIVE', 'STATE_LABEL')) AND columns.column_name NOT IN(SELECT dataElement.column_name FROM etk_data_element dataElement WHERE dataElement.data_object_id = dataObject.data_object_id AND ISNULL(dataElement.table_name, '') = '') AND columns.column_name NOT IN(SELECT dataElement.column_name + '_UID' FROM etk_data_element dataElement WHERE dataElement.data_object_id = dataObject.data_object_id AND ISNULL(dataElement.table_name, '') = '' AND dataElement.data_type = 10) AND columns.column_name NOT IN(SELECT dataElement.column_name + '_DTS' FROM etk_data_element dataElement WHERE dataElement.data_object_id = dataObject.data_object_id AND ISNULL(dataElement.table_name, '') = '' AND dataElement.data_type = 10) AND NOT (EXISTS(SELECT * FROM etk_data_element dataElement WHERE dataElement.data_object_id = dataObject.data_object_id AND dataElement.stored_in_document_management = 1) AND columns.column_name IN ('ETK_DM_CONTAINER_ID', 'ETK_RESOURCE_CONTAINER')) ORDER BY dataObject.table_name, columns.column_name"
                                           : "SELECT dataObject.TABLE_NAME, columns.COLUMN_NAME FROM etk_data_object dataObject JOIN user_tab_cols columns ON columns.table_name = dataObject.table_name WHERE dataObject.tracking_config_id = (SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive ) AND columns.column_name NOT IN('ID') AND NOT (dataObject.object_type = 1 AND dataObject.base_object = 0 AND columns.column_name IN ('ID_BASE', 'ID_PARENT')) AND NOT (dataObject.base_object = 1 AND column_name IN('ID_HIERARCHY', 'ID_ARCHIVE', 'STATE_LABEL')) AND columns.column_name NOT IN (SELECT dataElement.column_name FROM etk_data_element dataElement WHERE dataElement.data_object_id = dataObject.data_object_id AND dataElement.table_name IS NULL ) AND columns.column_name NOT IN (SELECT dataElement.column_name || '_UID' FROM etk_data_element dataElement WHERE dataElement.data_object_id = dataObject.data_object_id AND dataElement.table_name IS NULL AND dataElement.data_type = 10 ) AND columns.column_name NOT IN (SELECT dataElement.column_name || '_DTS' FROM etk_data_element dataElement WHERE dataElement.data_object_id = dataObject.data_object_id AND dataElement.table_name IS NULL AND dataElement.data_type = 10 ) AND NOT (EXISTS (SELECT * FROM etk_data_element dataElement WHERE dataElement.data_object_id = dataObject.data_object_id AND dataElement.stored_in_document_management = 1 ) AND columns.column_name IN ('ETK_DM_CONTAINER_ID', 'ETK_RESOURCE_CONTAINER')) ORDER BY dataObject.table_name, columns.column_name")
                .fetchList());

        for(final Map<String, Object> column : unusedColumns){
            List<Map<String, Object>> indexes;

            indexes = Utility.isSqlServer(etk) ? etk.createSQL("SELECT indexes.NAME FROM sys.indexes indexes JOIN sys.index_columns indexColumns ON indexColumns.index_id = indexes.index_id AND indexColumns.object_id = indexes.object_id JOIN sys.columns columns ON columns.column_id = indexColumns.column_id AND columns.object_id = indexColumns.object_id JOIN sys.tables tables ON tables.object_id = columns.object_id WHERE columns.name = :columnName AND tables.name = :tableName ORDER BY indexes.name, indexes.index_id")
                                               .setParameter("tableName", column.get("TABLE_NAME"))
                                               .setParameter("columnName", column.get("COLUMN_NAME"))
                                               .fetchList()
                                               : Collections.<Map<String, Object>>emptyList();

                                               column.put("indexes", indexes);
        }

        response.put("unusedColumns", JsonUtilities.encode(unusedColumns));

        return response;
    }
}
