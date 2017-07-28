package net.micropact.aea.du.page.checkSimpleWorkflowCorruption;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.query.QueryUtility;
import net.micropact.aea.utility.DataObjectType;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * This serves as the Controller Code for a Page which displays any objects which appear to have a problem within
 * their Simple Workflow tables such as missing entries it ETK_WORKITEM.
 *
 * @author zachary.miller
 */
public class CheckSimpleWorkflowCorruptionController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();

            final List<Map<String, Object>> btos = etk.createSQL("SELECT dataObject.TABLE_NAME, dataObject.BUSINESS_KEY FROM etk_data_object dataObject WHERE dataObject.tracking_config_id = :trackingConfigId AND dataObject.base_object = 1 AND dataObject.object_type = :objectType ORDER BY BUSINESS_KEY")
                    .setParameter("trackingConfigId", Utility.getTrackingConfigIdCurrent(etk))
                    .setParameter("objectType", DataObjectType.TRACKING.getEntellitrakNumber())
                    .fetchList();

            final boolean useSimpleWorkflow = !"false".equals(etk.getSystemPreferenceService().loadPreference("useSimpleWorkflow"));

            List<Map<String, Object>> objectsMissingWorkItems;
            List<Map<String, Object>> objectsMissingRuntimes;

            if(useSimpleWorkflow){
                objectsMissingWorkItems = getMissingWorkItems(etk, btos);
                objectsMissingRuntimes = getMissingRuntimes(etk, btos);
            }else{
                objectsMissingWorkItems = Collections.emptyList();
                objectsMissingRuntimes = Collections.emptyList();
            }

            response.put("useSimpleWorkflow", JsonUtilities.encode(useSimpleWorkflow));
            response.put("recordsWithoutWorkItems", JsonUtilities.encode(objectsMissingWorkItems));
            response.put("recordsWithoutRuntimes", JsonUtilities.encode(objectsMissingRuntimes));

            return response;
        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * This method retrieves all BTO records which are missing ETK_WORKITEM records.
     *
     * @param etk entellitrak execution context
     * @param btos A list of all BTO data objects in the system. This is only passed so that it only needs to be queried
     *          for once within this page.
     * @return A list of all objects which are missing ETK_WORKITEM records.
     */
    private static List<Map<String, Object>> getMissingWorkItems(final ExecutionContext etk,
            final List<Map<String, Object>> btos){
        return getMissingRecordsByQuery(etk, btos, "SELECT obj.id FROM %s obj WHERE NOT EXISTS(SELECT * FROM etk_workitem workItem WHERE workItem.workitem_id = obj.id_workflow) ORDER BY obj.id");
    }

    /**
     * Retrieves a list of all records which appear to have ETK_WORKITEM records, but not ETK_WORKFLOW_RUNTIME records.
     * Records which have ETK_WORKITEM records are excluded from this query because they will show up in a separate
     * list and it should be less confusing to have records not show in both places.
     *
     * @param etk entellitrak execution context
     * @param btos A list of all BTO data objects in the system.
     * @return A list of records which are missing ETK_WORKFLOW_RUNTIME records
     */
    private static List<Map<String, Object>> getMissingRuntimes(final ExecutionContext etk, final List<Map<String, Object>> btos){
        return getMissingRecordsByQuery(etk, btos, "SELECT obj.id FROM %s obj WHERE EXISTS(SELECT * FROM etk_workitem workItem WHERE workItem.workitem_id = obj.id_workflow) AND NOT EXISTS(SELECT * FROM etk_workflow_runtime runtime WHERE runtime.workitem_id = obj.id_workflow) ORDER BY obj.id");
    }

    /**
     * Returns a list of records which meet a particular query criteria.
     *
     * @param etk entellitrak execution context
     * @param btos maps representing BTO objects
     * @param query A query in the form of a "format String", which will take the BTO table name as a single parameter
     * @return a list of all records which match the query criteria
     */
    private static List<Map<String, Object>> getMissingRecordsByQuery(final ExecutionContext etk,
            final List<Map<String, Object>> btos,
            final String query){
        final List<Map<String, Object>> returnList = new LinkedList<>();

        for(final Map<String, Object> bto : btos){
            final String tableName = (String) bto.get("TABLE_NAME");
            final String businessKey = (String) bto.get("BUSINESS_KEY");

            for(final Long objectId : QueryUtility.mapsToLongs(
                    etk.createSQL(String.format(query,
                            tableName))
                    .fetchList())){
                returnList.add(Utility.arrayToMap(String.class, Object.class, new Object[][]{
                    {"businessKey", businessKey},
                    {"trackingId", objectId}
                }));
            }
        }
        return returnList;
    }
}
