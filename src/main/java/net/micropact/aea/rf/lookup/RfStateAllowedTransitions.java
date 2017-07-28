package net.micropact.aea.rf.lookup;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.lookup.LookupExecutionContext;
import com.entellitrak.lookup.LookupHandler;

import net.entellitrak.aea.lookup.IAeaLookupHandler;
import net.micropact.aea.utility.Utility;

/**
 * This lookup is intended specifically for use with the Allowed Transitions field of the RF State object.
 * @author zmiller
 */
public class RfStateAllowedTransitions implements LookupHandler, IAeaLookupHandler {

    @Override
    public String execute(final LookupExecutionContext etk)
            throws ApplicationException {
        if(etk.isForTracking()){
            return Utility.isSqlServer(etk) ? "SELECT transition.id Value, transition.c_name + ' (' + transition.c_code + ')' Display FROM t_rf_transition transition WHERE transition.id_base = {?baseId} ORDER BY transition.c_order, Display, Value"
                    : "SELECT transition.id Value, transition.c_name || ' (' || transition.c_code || ')' Display FROM t_rf_transition transition WHERE transition.id_base = {?baseId} ORDER BY transition.c_order, Display, Value";
        }else{
            throw new ApplicationException("net.micropact.aea.rf.lookup.RfStateAllowedTransitions.execute(LookupExecutionContext) is only applicable to forTracking");
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
