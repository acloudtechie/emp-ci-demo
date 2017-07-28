package net.micropact.aea.rf.lookup;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.lookup.LookupExecutionContext;
import com.entellitrak.lookup.LookupHandler;

import net.entellitrak.aea.lookup.IAeaLookupHandler;
import net.micropact.aea.utility.Utility;

/**
 * This lookup handler is intended for use specifically for the Transition field of the RF Workflow Effect object.
 * @author zmiller
 */
public class RfWorkflowEffectRfTransition implements LookupHandler, IAeaLookupHandler {

    @Override
    public String execute(final LookupExecutionContext etk)
            throws ApplicationException {

        if(etk.isForTracking()){
            return Utility.isSqlServer(etk) ? "SELECT id Value, c_name + ' (' + c_code + ')' Display FROM t_rf_transition WHERE id_parent = {?parentId} ORDER BY c_order, Display, Value"
                    : "SELECT id Value, c_name || ' (' || c_code || ')' Display FROM t_rf_transition WHERE id_parent = {?parentId} ORDER BY c_order, Display, Value";
        }else if(etk.isForSearch()){
            return Utility.isSqlServer(etk) ? "SELECT rfTransition.id Value, rfWorkflow.c_name + ' - ' + rfTransition.c_name + ' (' + rfTransition.c_code + ')' Display FROM t_rf_workflow rfWorkflow JOIN t_rf_transition rfTransition ON rfTransition.id_parent = rfWorkflow.id WHERE rfTransition.id IN (SELECT effectTransition.c_transition FROM m_rf_effect_transition effectTransition ) ORDER BY Display, Value"
                                              : "SELECT rfTransition.id Value, rfWorkflow.c_name || ' - ' || rfTransition.c_name || ' (' || rfTransition.c_code || ')' Display FROM t_rf_workflow rfWorkflow JOIN t_rf_transition rfTransition ON rfTransition.id_parent = rfWorkflow.id WHERE rfTransition.id IN (SELECT effectTransition.c_transition FROM m_rf_effect_transition effectTransition ) ORDER BY Display, Value";
        }else if(etk.isForAdvancedSearch()){
            return Utility.isSqlServer(etk) ? "SELECT rfTransition.id Value, rfWorkflow.c_name + ' - ' + rfTransition.c_name + ' (' + rfTransition.c_code + ')' Display FROM t_rf_workflow rfWorkflow JOIN t_rf_transition rfTransition ON rfTransition.id_parent = rfWorkflow.id ORDER BY Display, Value"
                                              : "SELECT rfTransition.id Value, rfWorkflow.c_name || ' - ' || rfTransition.c_name || ' (' || rfTransition.c_code || ')' Display FROM t_rf_workflow rfWorkflow JOIN t_rf_transition rfTransition ON rfTransition.id_parent = rfWorkflow.id ORDER BY Display, Value";
        }else if(etk.isForView()){
            return Utility.isSqlServer(etk) ? "SELECT id Value, c_name + ' (' + c_code + ')' Display FROM t_rf_transition"
                    : "SELECT id Value, c_name || ' (' || c_code || ')' Display FROM t_rf_transition";
        }else if(etk.isForSingleResult()){
            return Utility.isSqlServer(etk) ? "SELECT transition.id Value, transition.c_name + ' (' + transition.c_code + ')' Display FROM m_rf_effect_transition effectTransition JOIN t_rf_transition transition ON transition.id = effectTransition.c_transition WHERE effectTransition.id_owner = {?trackingId} ORDER BY transition.c_order, Display, Value"
                    : "SELECT transition.id Value, transition.c_name || ' (' || transition.c_code || ')' Display FROM m_rf_effect_transition effectTransition JOIN t_rf_transition transition ON transition.id = effectTransition.c_transition WHERE effectTransition.id_owner = {?trackingId} ORDER BY transition.c_order, Display, Value";
        }else{
            throw new ApplicationException("net.micropact.aea.rf.lookup.RfTransition.execute(LookupExecutionContext) is only applicable to forTracking, forView, forSearch, forAdvancedSearch, forSingleResult");
        }
    }

    @Override
    public String getValueTableName(final ExecutionContext theExecutionContext) {
        return "T_RF_TRANSITION";
    }

    @Override
    public String getValueColumnName(final ExecutionContext theExecutionContext) {
        return "ID";
    }
}
