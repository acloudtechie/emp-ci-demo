package net.micropact.aea.du.page.cleanOrphanedMTableEntries;

import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * This serves as the controller code for a page which deletes records from entellitrak M_ tables which
 * are orphaned because their ID_OWNER record no longer exists.
 *
 * @author ahargrave 09/09/2016
 */
public class CleanOrphanedMTableEntriesController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();

            final List<Map<String, Object>> mTables = etk.createSQL("SELECT do.table_name OBJECT_TABLE, de.table_name M_TABLE FROM etk_data_object do JOIN etk_data_element de ON de.data_object_id = do.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND /* This case statement is here because core uses '' instead of NULL in SQL Server and because SQL Server and Oracle treat '' differently */ CASE WHEN de.table_name = '' THEN NULL ELSE de.table_name END IS NOT NULL ORDER BY OBJECT_TABLE, M_TABLE")
                    .setParameter("trackingConfigId", Utility.getTrackingConfigIdCurrent(etk))
                    .fetchList();

            long totalCount = 0;
            for (final Map<String, Object> tableSet : mTables) {
                final String objectTable = (String) tableSet.get("OBJECT_TABLE");
                final String mTable = (String) tableSet.get("M_TABLE");
                final long recordCount = etk.createSQL(String.format("DELETE FROM %s WHERE ID_OWNER NOT IN (SELECT ID FROM %s)",
                        mTable,
                        objectTable))
                        .execute();
                totalCount += recordCount;
            }
            response.put("count", JsonUtilities.encode(totalCount));
            return response;

        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }
}
