package net.micropact.aea.du.page.unusedTables;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.Utility;

/**
 * This page is used for displaying tables which exist in the database but do not appear to be being
 * used by entellitrak.
 * This commonly occurs when a data object is deleted from the front end because entellitrak will not drop the table.
 *
 * @author zmiller
 */
public class UnusedTablesController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {

        final TextResponse response = etk.createTextResponse();

        response.put(
                "unusedTables",
                etk.createSQL(
                        Utility.isSqlServer(etk) ? "SELECT tables.TABLE_NAME FROM information_schema.tables tables WHERE tables.table_name NOT IN(SELECT table_name FROM( SELECT dataObject.table_name FROM etk_data_object dataObject WHERE dataObject.tracking_config_id = (SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive) UNION SELECT dataElement.table_name FROM etk_data_object dataObject JOIN etk_data_element dataElement ON dataElement.data_object_id = dataObject.data_object_id WHERE dataObject.tracking_config_id = (SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive) ) etkTables WHERE ISNULL(etkTables.table_name, '') != '') AND tables.table_name NOT LIKE 'JBPM[_]%' AND tables.table_name NOT LIKE 'ETK[_]%' AND tables.table_name NOT LIKE 'ETKM[_]%' AND tables.table_type = 'BASE TABLE' AND tables.table_name NOT IN('sysdiagrams') ORDER BY tables.table_name"
                                                   : "SELECT tables.TABLE_NAME FROM user_tables tables WHERE tables.table_name NOT IN (SELECT table_name FROM (SELECT dataObject.table_name FROM etk_data_object dataObject WHERE dataObject.tracking_config_id = (SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive ) UNION SELECT dataElement.table_name FROM etk_data_object dataObject JOIN etk_data_element dataElement ON dataElement.data_object_id = dataObject.data_object_id WHERE dataObject.tracking_config_id = (SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive ) ) etkTables WHERE etkTables.table_name IS NOT NULL ) AND tables.table_name NOT LIKE 'JBPM/_%' ESCAPE '/' AND tables.table_name NOT LIKE 'ETK/_%' ESCAPE '/' AND tables.table_name NOT LIKE 'ETKM/_%' ESCAPE '/' ORDER BY tables.table_name")
                .fetchJSON());

        return response;
    }
}
