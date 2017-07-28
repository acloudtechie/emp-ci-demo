package net.micropact.aea.rf.lookup;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.lookup.LookupExecutionContext;
import com.entellitrak.lookup.LookupHandler;

import net.entellitrak.aea.lookup.IAeaLookupHandler;
import net.micropact.aea.utility.Utility;

/**
 * This lookup is intended specifically for use in the From States field on the RF Transition object.
 * @author zmiller
 */
public class RfTransitionFromStates implements LookupHandler, IAeaLookupHandler {

    @Override
    public String execute(final LookupExecutionContext etk)
            throws ApplicationException {
        if(etk.isForTracking()){
            return Utility.isSqlServer(etk) ? "SELECT rfState.id Value, rfState.c_name + ' (' + rfState.c_code + ')' Display FROM t_rf_state rfState WHERE rfState.id_parent = {?parentId} ORDER BY rfState.c_order, Display, Value"
                    : "SELECT rfState.id Value, rfState.c_name || ' (' || rfState.c_code || ')' Display FROM t_rf_state rfState WHERE rfState.id_parent = {?parentId} ORDER BY rfState.c_order, Display, Value";
        }else if(etk.isForAdvancedSearch()){
            return Utility.isSqlServer(etk) ? "SELECT rfState.id Value, rfWorkflow.c_name + ' - ' + rfState.c_name + ' (' + rfState.c_code + ')' Display FROM t_rf_state rfState JOIN t_rf_workflow rfWorkflow ON rfWorkflow.id = rfState.id_parent ORDER BY Display, Value"
                      : "SELECT rfState.id Value, rfWorkflow.c_name || ' - ' || rfState.c_name || ' (' || rfState.c_code || ')' Display FROM t_rf_state rfState JOIN t_rf_workflow rfWorkflow ON rfWorkflow.id = rfState.id_parent ORDER BY Display, Value";
        }else if(etk.isForSearch()){
            return Utility.isSqlServer(etk) ? "SELECT rfState.id Value, rfWorkflow.c_name + ' - ' + rfState.c_name + ' (' + rfState.c_code + ')' Display FROM t_rf_state rfState JOIN t_rf_workflow rfWorkflow ON rfWorkflow.id = rfState.id_parent WHERE rfState.id IN (SELECT c_from_state FROM m_rf_transition_from_state ) ORDER BY Display, Value"
                    : "SELECT rfState.id Value, rfWorkflow.c_name || ' - ' || rfState.c_name || ' (' || rfState.c_code || ')' Display FROM t_rf_state rfState JOIN t_rf_workflow rfWorkflow ON rfWorkflow.id = rfState.id_parent WHERE rfState.id IN (SELECT c_from_state FROM m_rf_transition_from_state ) ORDER BY Display, Value";
        }else if(etk.isForView()){
            return Utility.isSqlServer(etk) ? "SELECT rfState.id Value, rfState.c_name + ' (' + rfState.c_code + ')' Display FROM t_rf_state rfState"
                    : "SELECT rfState.id Value, rfState.c_name || ' (' || rfState.c_code || ')' Display FROM t_rf_state rfState";
        }else if(etk.isForSingleResult()){
            return Utility.isSqlServer(etk) ? "SELECT rfState.id Value, rfState.c_name + ' (' + rfState.c_code + ')' Display FROM m_rf_transition_from_state rfTransitionFromState JOIN t_rf_state rfState ON rfState.id = rfTransitionFromState.c_from_state WHERE rfTransitionFromState.id_owner = {?trackingId} ORDER BY rfState.c_order, Display, Value"
                    : "SELECT rfState.id Value, rfState.c_name || ' (' || rfState.c_code || ')' Display FROM m_rf_transition_from_state rfTransitionFromState JOIN t_rf_state rfState ON rfState.id = rfTransitionFromState.c_from_state WHERE rfTransitionFromState.id_owner = {?trackingId} ORDER BY rfState.c_order, Display, Value";
        }else{
            throw new ApplicationException("net.micropact.aea.rf.RfTransitionFromStates.execute(LookupExecutionContext) is only applicable to forTracking, forSearch, forView, forAdvancedSearch, forSingleResult");
        }
    }

    @Override
    public String getValueTableName(final ExecutionContext theExecutionContext) {
        return "T_RF_STATE";
    }

    @Override
    public String getValueColumnName(final ExecutionContext theExecutionContext) {
        return "ID";
    }
}
