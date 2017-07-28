package net.micropact.aea.rf.lookup;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.lookup.LookupExecutionContext;
import com.entellitrak.lookup.LookupHandler;

import net.entellitrak.aea.lookup.IAeaLookupHandler;
import net.micropact.aea.utility.Utility;

/**
 * This lookup is intended solely for use for the Child Transition Element field on the RF Workflow object.
 * @author zmiller
 */
public class RfWorkflowChildTransitionElement implements LookupHandler, IAeaLookupHandler {

    @Override
    public String execute(final LookupExecutionContext etk)
            throws ApplicationException {
        if(etk.isForTracking()){
            return "SELECT de.business_key Value, de.name Display FROM etk_data_object DO JOIN etk_data_element de ON de.data_object_id = do.data_object_id WHERE do.tracking_config_id = ( SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive ) AND do.business_key = {?childObject} AND de.bound_to_lookup = 1 AND NULLIF(de.table_name, '') IS NULL ORDER BY Display, Value";
        }else if (etk.isForSearch() || etk.isForAdvancedSearch()){
            return Utility.isSqlServer(etk) ? "SELECT dataElement.business_key Value, dataElement.name + ' (' + dataObject.name + ')' Display FROM etk_data_element dataElement JOIN etk_data_object dataObject ON dataObject.data_object_id = dataElement.data_object_id WHERE dataObject.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive) AND dataElement.business_key IN (SELECT c_child_transition_element FROM t_rf_workflow) ORDER BY Display, Value"
                   : "SELECT dataElement.business_key Value, dataElement.name || ' (' || dataObject.name || ')' Display FROM etk_data_element dataElement JOIN etk_data_object dataObject ON dataObject.data_object_id = dataElement.data_object_id WHERE dataObject.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive) AND dataElement.business_key IN (SELECT c_child_transition_element FROM t_rf_workflow) ORDER BY Display, Value";
        }else if (etk.isForView()){
            return "SELECT de.business_key Value, de.name Display FROM etk_data_object do JOIN etk_data_element de ON de.data_object_id = do.data_object_id WHERE do.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive)";
        }else if(etk.isForSingleResult()){
            return "SELECT dataElement.business_key Value, dataElement.name Display FROM t_rf_workflow rfWorkflow JOIN etk_data_element dataElement ON dataElement.business_key = rfWorkflow.c_child_transition_element JOIN etk_data_object dataObject ON dataObject.data_object_id = dataElement.data_object_id WHERE dataObject.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive) AND rfWorkflow.id = {?trackingId}";
        }else{
            throw new ApplicationException("net.micropact.aea.rf.lookup.RfWorkflowChildTransitionElement.execute(LookupExecutionContext) is only applicable forTracking, forSearch, forView, forAdvancedSearch, forSingleResult");
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
