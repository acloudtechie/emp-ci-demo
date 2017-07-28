package net.micropact.aea.rf.lookup;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.lookup.LookupExecutionContext;
import com.entellitrak.lookup.LookupHandler;

import net.entellitrak.aea.lookup.IAeaLookupHandler;
import net.micropact.aea.utility.Utility;

/**
 * This lookup is intended specifically for use for the Child Object field on the RF Workflow object.
 * @author zmiller
 */
public class RfWorkflowChildObject implements LookupHandler, IAeaLookupHandler {

    @Override
    public String execute(final LookupExecutionContext etk)
            throws ApplicationException {

        if(etk.isForTracking()){
            return Utility.isSqlServer(etk) ? "SELECT childObject.business_key Value, parentObject.object_name + ' > ' + childObject.object_name Display FROM etk_data_object childObject JOIN etk_data_object parentObject ON parentObject.data_object_id = childObject.parent_object_id WHERE childObject.tracking_config_id = ( SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive ) AND childObject.object_type = 1 ORDER BY Display"
                    : "SELECT childObject.business_key Value, parentObject.object_name || ' > ' || childObject.object_name Display FROM etk_data_object childObject JOIN etk_data_object parentObject ON parentObject.data_object_id = childObject.parent_object_id WHERE childObject.tracking_config_id = ( SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive ) AND childObject.object_type = 1 ORDER BY Display";
        }else if(etk.isForSearch() || etk.isForAdvancedSearch()){
            return Utility.isSqlServer(etk) ? "SELECT childObject.business_key Value, parentObject.object_name + ' > ' + childObject.object_name Display FROM etk_data_object childObject JOIN etk_data_object parentObject ON parentObject.data_object_id = childObject.parent_object_id WHERE childObject.tracking_config_id = (SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive ) AND childObject.business_key IN (SELECT c_child_object FROM t_rf_workflow) ORDER BY Display, Value "
                    : "SELECT childObject.business_key Value, parentObject.object_name || ' > ' || childObject.object_name Display FROM etk_data_object childObject JOIN etk_data_object parentObject ON parentObject.data_object_id = childObject.parent_object_id WHERE childObject.tracking_config_id = (SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive ) AND childObject.business_key IN (SELECT c_child_object FROM t_rf_workflow) ORDER BY Display, Value ";
        }else if(etk.isForView()){
            return Utility.isSqlServer(etk) ? "SELECT childObject.business_key Value, parentObject.object_name + ' > ' + childObject.object_name Display FROM etk_data_object childObject JOIN etk_data_object parentObject ON parentObject.data_object_id = childObject.parent_object_id WHERE childObject.tracking_config_id = ( SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive ) AND childObject.object_type = 1"
                    : "SELECT childObject.business_key Value, parentObject.object_name || ' > ' || childObject.object_name Display FROM etk_data_object childObject JOIN etk_data_object parentObject ON parentObject.data_object_id = childObject.parent_object_id WHERE childObject.tracking_config_id = ( SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive ) AND childObject.object_type = 1";
        }else if(etk.isForSingleResult()){
            return Utility.isSqlServer(etk) ? "SELECT childObject.business_key Value, parentObject.object_name + ' > ' + childObject.object_name Display FROM t_rf_workflow rfWorkflow JOIN etk_data_object childObject ON childObject.business_key = rfWorkflow.c_child_object JOIN etk_data_object parentObject ON parentObject.data_object_id = childObject.parent_object_id WHERE childObject.tracking_config_id = (SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive) AND rfWorkflow.id = {?trackingId}"
                    : "SELECT childObject.business_key Value, parentObject.object_name || ' > ' || childObject.object_name Display FROM t_rf_workflow rfWorkflow JOIN etk_data_object childObject ON childObject.business_key = rfWorkflow.c_child_object JOIN etk_data_object parentObject ON parentObject.data_object_id = childObject.parent_object_id WHERE childObject.tracking_config_id = (SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive) AND rfWorkflow.id = {?trackingId}";
        }else{
            throw new ApplicationException("net.micropact.aea.rf.lookup.RfWorkflowChildObject.execute(LookupExecutionContext) is only applicable forTracking, forSearch, forView, forAdvancedSearch, forSingleResult");
        }
    }

    @Override
    public String getValueTableName(final ExecutionContext theExecutionContext) {
        return "ETK_DATA_OBJECT";
    }

    @Override
    public String getValueColumnName(final ExecutionContext theExecutionContext) {
        return "BUSINESS_KEY";
    }
}
