package net.micropact.aea.du.page.viewBadDates;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.DataElementType;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * This class serves as the controller code for a page which can be used to view Date fields which have a timestamp
 * portion.
 *
 * @author zachary.miller
 */
public class ViewBadDatesController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();

            final List<Map<String, Object>> badRecords = new LinkedList<>();

            for(final Map<String, Object> dateElement : etk.createSQL("SELECT do.TABLE_NAME, do.OBJECT_TYPE, de.COLUMN_NAME, de.name ELEMENT_NAME, de.business_key ELEMENT_BUSINESS_KEY FROM etk_data_object DO JOIN etk_data_element de ON de.data_object_id = do.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.data_type = :dateDataType AND de.table_name IS NULL ORDER BY ELEMENT_BUSINESS_KEY")
                    .setParameter("trackingConfigId", Utility.getTrackingConfigIdCurrent(etk))
                    .setParameter("dateDataType", DataElementType.DATE.getEntellitrakNumber())
                    .fetchList()){

                final String tableName = (String) dateElement.get("TABLE_NAME");
                final String columnName = (String) dateElement.get("COLUMN_NAME");
                final int total = etk.createSQL(String.format(
                        Utility.isSqlServer(etk) ? "SELECT COUNT(*) TOTAL FROM %s WHERE %s != CAST(%s AS DATE)"
                                                   : "SELECT COUNT(*) TOTAL FROM %s WHERE %s != trunc(%s)",
                        tableName,
                        columnName,
                        columnName))
                        .fetchInt();
                if(total > 0){
                    dateElement.put("total", total);
                    dateElement.put("query", generateRecordsQuery(etk, tableName, columnName));
                    badRecords.add(dateElement);
                }
            }

            response.put("badRecords", JsonUtilities.encode(badRecords));

            return response;
        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Generates a SQL query which will return the specific records which have an issue with a specific date field.
     *
     * @param etk entellitrak execution context
     * @param tableName table name of the table containing the date field
     * @param columnName the column which holds the date field
     * @return the SQL query which will return the records.
     */
    private static String generateRecordsQuery(
            final PageExecutionContext etk,
            final String tableName,
            final String columnName) {
        return String.format(Utility.isSqlServer(etk) ? "SELECT * FROM %s WHERE %s != CAST(%s AS DATE)"
                                         : "SELECT * FROM %s WHERE %s != TRUNC(%s) ORDER BY id",
                             tableName,
                             columnName,
                             columnName);
    }
}
