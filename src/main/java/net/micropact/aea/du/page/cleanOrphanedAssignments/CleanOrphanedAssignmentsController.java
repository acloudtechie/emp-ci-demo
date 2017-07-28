package net.micropact.aea.du.page.cleanOrphanedAssignments;

import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.Utility;

/**
 * <p>
 *  This page cleans out rows in the ETK_ASSIGNMENT table which link to objects which no longer exist.
 * </p>
 * <p>
 *  Common ways for this to happen are that that particular object was deleted, or that the entire data object was
 *  deleted from configuration.
 * </p>
 * @author zmiller
 */
public class CleanOrphanedAssignmentsController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();

            deleteStaticlyLinkedData(etk);
            deleteByOrphanedTrackingIds(etk);

            return response;
        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Deletes assignment records where the tracking_id does not reference an object of the correct type in the system.
     *
     * @param etk entellitrak execution context
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static void deleteByOrphanedTrackingIds(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException {
        final List<Map<String, Object>> dataObjects = etk.createSQL("SELECT TABLE_NAME FROM etk_data_object do WHERE do.tracking_config_id = :trackingConfigId AND base_object = 1 AND object_type = 1")
                .setParameter("trackingConfigId", Utility.getTrackingConfigIdCurrent(etk))
                .fetchList();

        for(final Map<String, Object> dataObject : dataObjects){
            etk.createSQL(String.format("DELETE FROM etk_assignment WHERE etk_assignment.data_object_key = :tableName AND NOT EXISTS(SELECT * FROM %s obj WHERE obj.id = etk_assignment.tracking_id)",
                    dataObject.get("TABLE_NAME")))
            .setParameter("tableName", dataObject.get("TABLE_NAME"))
            .execute();
        }
    }

    /**
     * Deletes data which can be cleared out in a single static query. Such as orphaned roles and subjects, but not data
     * which needs dynamically generated queries such as orphaned tracking ids.
     *
     * @param etk entellitrak execution context
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static void deleteStaticlyLinkedData(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException {
        etk.createSQL("DELETE FROM etk_assignment WHERE NOT EXISTS (SELECT * FROM etk_role r WHERE r.role_id = etk_assignment.role_id) OR etk_assignment.subject_id NOT IN ( SELECT u.user_id FROM etk_user u UNION SELECT g.group_id FROM etk_group g ) OR data_object_key NOT IN( SELECT dataObject.table_name FROM etk_data_object dataObject WHERE dataObject.tracking_config_id = :trackingConfigId AND dataObject.object_type = 1 AND dataObject.base_object = 1 )")
        .setParameter("trackingConfigId", Utility.getTrackingConfigIdCurrent(etk))
        .execute();
    }
}
