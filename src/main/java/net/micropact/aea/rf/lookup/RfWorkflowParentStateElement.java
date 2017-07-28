package net.micropact.aea.rf.lookup;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.lookup.LookupExecutionContext;
import com.entellitrak.lookup.LookupHandler;

import net.entellitrak.aea.lookup.IAeaLookupHandler;
import net.micropact.aea.utility.Utility;

/**
 * This lookup is intended specifically for use in the Parent State Element data element on the RF Workflow data object.
 * @author zmiller
 */
public class RfWorkflowParentStateElement implements LookupHandler, IAeaLookupHandler {

    @Override
    public String execute(final LookupExecutionContext etk)
            throws ApplicationException {
        if(etk.isForTracking()){
            return "SELECT parentElement.business_key Value, parentElement.name Display FROM etk_data_object parentObject JOIN etk_data_object childObject ON childObject.parent_object_id = parentObject.data_object_id JOIN etk_data_element parentElement ON parentElement.data_object_id = parentObject.data_object_id WHERE parentObject.tracking_config_id = ( SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive ) AND childObject.business_key = {?childObject} AND parentElement.bound_to_lookup = 1 AND NULLIF(parentElement.table_name, '') IS NULL ORDER BY Display";
        }else if(etk.isForSearch() || etk.isForAdvancedSearch()){
            return Utility.isSqlServer(etk) ? "SELECT parentElement.business_key Value, parentElement.name + '(' + parentObject.name + ')' Display FROM etk_data_element parentElement JOIN etk_data_object parentObject ON parentObject.data_object_id = parentElement.data_object_id WHERE parentObject.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive) AND parentElement.business_key IN(SELECT c_parent_state_element FROM t_rf_workflow) ORDER BY Display, Value"
                           : "SELECT parentElement.business_key Value, parentElement.name || '(' || parentObject.name || ')' Display FROM etk_data_element parentElement JOIN etk_data_object parentObject ON parentObject.data_object_id = parentElement.data_object_id WHERE parentObject.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive) AND parentElement.business_key IN(SELECT c_parent_state_element FROM t_rf_workflow) ORDER BY Display, Value";
        }else if(etk.isForView()){
            return "SELECT parentElement.business_key Value, parentElement.name Display FROM etk_data_element parentElement JOIN etk_data_object parentObject ON parentObject.data_object_id = parentElement.data_object_id WHERE parentObject.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive)";
        }else if(etk.isForSingleResult()){
            return "SELECT parentElement.business_key Value, parentElement.name Display FROM t_rf_workflow rfWorkflow JOIN etk_data_element parentElement ON parentElement.business_key = rfWorkflow.c_parent_state_element JOIN etk_data_object parentObject ON parentObject.data_object_id = parentElement.data_object_id WHERE parentObject.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive) AND rfWorkflow.id = {?trackingId} ";
        }else{
            throw new ApplicationException("net.micropact.aea.rf.lookup.RfWorkflowParentStateElement.execute(LookupExecutionContext) is only applicable forTracking, forSearch, forView, forAdvancedSearch, forSingleResult");
        }
    }

    @Override
    public String getValueTableName(final ExecutionContext theExecutionContext) {
        return "ETK_DATA_ELEMENT";
    }

    @Override
    public String getValueColumnName(final ExecutionContext theExecutionContext) {
        return "BUSINESS_KEY";
    }
}
