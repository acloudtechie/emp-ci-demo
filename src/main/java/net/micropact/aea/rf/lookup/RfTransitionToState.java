package net.micropact.aea.rf.lookup;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.lookup.LookupExecutionContext;
import com.entellitrak.lookup.LookupHandler;

import net.entellitrak.aea.lookup.IAeaLookupHandler;
import net.micropact.aea.utility.Utility;

/**
 * This lookup is intended specifically for use for the To State field of the RF Transition object.
 * @author zmiller
 */
public class RfTransitionToState implements LookupHandler, IAeaLookupHandler {

    @Override
    public String execute(final LookupExecutionContext etk)
            throws ApplicationException {
        if(etk.isForTracking()){
            return Utility.isSqlServer(etk) ? "SELECT rfState.id Value, rfState.c_name + ' (' + rfState.c_code + ')' Display FROM t_rf_state rfState WHERE rfState.id_parent = {?parentId} ORDER BY rfState.c_order, Display, Value"
                    : "SELECT rfState.id Value, rfState.c_name || ' (' || rfState.c_code || ')' Display FROM t_rf_state rfState WHERE rfState.id_parent = {?parentId} ORDER BY rfState.c_order, Display, Value";
        }else if(etk.isForSearch()){
            return Utility.isSqlServer(etk) ? "SELECT rfState.id Value, rfWorkflow.c_name + ' - ' + rfState.c_name + ' (' + rfState.c_code + ')' Display FROM t_rf_workflow rfWorkflow JOIN t_rf_state rfState ON rfState.id_parent = rfWorkflow.id WHERE rfState.id IN (SELECT c_to_state FROM t_rf_transition ) ORDER BY Display, Value "
                                              : "SELECT rfState.id Value, rfWorkflow.c_name || ' - ' || rfState.c_name || ' (' || rfState.c_code || ')' Display FROM t_rf_workflow rfWorkflow JOIN t_rf_state rfState ON rfState.id_parent = rfWorkflow.id WHERE rfState.id IN (SELECT c_to_state FROM t_rf_transition ) ORDER BY Display, Value ";
        }else if(etk.isForAdvancedSearch()){
            return Utility.isSqlServer(etk) ? "SELECT rfState.id Value, rfWorkflow.c_name + ' - ' + rfState.c_name + ' (' + rfState.c_code + ')' Display FROM t_rf_workflow rfWorkflow JOIN t_rf_state rfState ON rfState.id_parent = rfWorkflow.id ORDER BY Display, Value "
                                              : "SELECT rfState.id Value, rfWorkflow.c_name || ' - ' || rfState.c_name || ' (' || rfState.c_code || ')' Display FROM t_rf_workflow rfWorkflow JOIN t_rf_state rfState ON rfState.id_parent = rfWorkflow.id ORDER BY Display, Value ";
        }else if (etk.isForView()){
            return Utility.isSqlServer(etk) ? "SELECT rfState.id Value, rfState.c_name + ' (' + rfState.c_code + ')' Display FROM t_rf_state rfState"
                    : "SELECT rfState.id Value, rfState.c_name || ' (' || rfState.c_code || ')' Display FROM t_rf_state rfState";
        }else if(etk.isForSingleResult()){
            return Utility.isSqlServer(etk) ? "SELECT rfState.id Value, rfState.c_name + ' (' + rfState.c_code + ')' Display FROM t_rf_transition rfTransition JOIN t_rf_state rfState ON rfState.id = rfTransition.c_to_state WHERE rfTransition.id = {?trackingId}"
                    : "SELECT rfState.id Value, rfState.c_name || ' (' || rfState.c_code || ')' Display FROM t_rf_transition rfTransition JOIN t_rf_state rfState ON rfState.id = rfTransition.c_to_state WHERE rfTransition.id = {?trackingId}";
        }else{
            throw new ApplicationException("net.micropact.aea.rf.lookup.RfTransitionToStates is only applicable forTracking, forSearch, forView, forAdvancedSearch, forSingleResult");
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
