package net.micropact.aea.eu.lookup;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.lookup.LookupExecutionContext;
import com.entellitrak.lookup.LookupHandler;

import net.entellitrak.aea.lookup.IAeaLookupHandler;
import net.micropact.aea.utility.Utility;

/**
 * Lookup handler to be used for the EuEmailQueue.fromAddress data element.
 *
 * @author Zachary.Miller
 */
public class EuEmailQueueFromAddress implements LookupHandler, IAeaLookupHandler {

    @Override
    public String execute(final LookupExecutionContext etk) throws ApplicationException {
        if(etk.isForView()){
            return Utility.isSqlServer(etk) ? "SELECT id Value, ISNULL(c_address, '') + ' <' + ISNULL(c_personal, '') + '>' Display FROM t_eu_queue_address"
                                              : "SELECT id Value, c_address || ' <' || c_personal || '>' Display FROM t_eu_queue_address";
        }else if(etk.isForSingleResult()){
            return Utility.isSqlServer(etk) ? "SELECT id Value, ISNULL(c_address, '') + ' <' + ISNULL(c_personal, '') + '>' Display FROM t_eu_queue_address WHERE id = CASE WHEN {?fromAddress} != '' THEN {?fromAddress} END"
                                              : "SELECT id Value, c_address || ' <' || c_personal || '>' Display FROM t_eu_queue_address WHERE id = {?fromAddress}";
        }else{
            throw new ApplicationException(String.format("%s not applicable in this context",
                    getClass().getName()));
        }
    }

    @Override
    public String getValueColumnName(final ExecutionContext etk) {
        return "ID";
    }

    @Override
    public String getValueTableName(final ExecutionContext etk) {
        return "T_EU_QUEUE_ADDRESS";
    }
}
