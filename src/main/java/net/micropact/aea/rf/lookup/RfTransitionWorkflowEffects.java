package net.micropact.aea.rf.lookup;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.lookup.LookupExecutionContext;
import com.entellitrak.lookup.LookupHandler;

import net.entellitrak.aea.lookup.IAeaLookupHandler;

/**
 * This lookup is intended specifically for use with the Workflow Effect field on the RF Transition Effect object.
 * @author zmiller
 */
public class RfTransitionWorkflowEffects implements LookupHandler, IAeaLookupHandler {

    @Override
    public String execute(final LookupExecutionContext etk)
            throws ApplicationException {

        if(etk.isForTracking()){
            return "SELECT workflowEffect.id Value, workflowEffect.c_name Display FROM t_rf_workflow_effect workflowEffect WHERE workflowEffect.id_base = {?baseId} ORDER BY Display, Value";
        }else{
            throw new ApplicationException("net.micropact.aea.rf.lookup.RfTransitionWorkflowEffects.execute(LookupExecutionContext is only applicable to forTracking");
        }
    }

    @Override
    public String getValueTableName(final ExecutionContext theExecutionContext) {
        return "T_RF_WORKFLOW_EFFECT";
    }

    @Override
    public String getValueColumnName(final ExecutionContext theExecutionContext) {
        return "ID";
    }
}
