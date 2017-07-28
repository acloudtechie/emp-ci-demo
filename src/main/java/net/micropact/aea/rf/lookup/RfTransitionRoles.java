package net.micropact.aea.rf.lookup;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.lookup.LookupExecutionContext;
import com.entellitrak.lookup.LookupHandler;

import net.entellitrak.aea.lookup.IAeaLookupHandler;

/**
 * This lookup handler returns a list of all roles in the system.
 * @author zmiller
 */
public class RfTransitionRoles implements LookupHandler, IAeaLookupHandler {

    @Override
    public String execute(final LookupExecutionContext etk)
            throws ApplicationException {
        if(etk.isForTracking()){
            return "SELECT role_id Value, name Display FROM etk_role ORDER BY Display, Value";
        }else if(etk.isForView()){
            return "SELECT role_id Value, name Display FROM etk_role";
        }else if(etk.isForSearch()){
            return "SELECT role_id Value, name Display FROM etk_role WHERE role_id IN (SELECT c_role FROM m_rf_transition_role) ORDER BY Display, Value";
        }else if(etk.isForAdvancedSearch()){
            return "SELECT role_id Value, name Display FROM etk_role ORDER BY Display, Value";
        }else if(etk.isForSingleResult()){
            return "SELECT role.role_id Value, role.name Display FROM m_rf_transition_role transitionRole JOIN etk_role role ON role.role_id = transitionRole.c_role WHERE transitionRole.id_owner = {?trackingId} ORDER BY Display, Value";
        }else{
            throw new ApplicationException("net.micropact.aea.rf.lookup.Role.execute(LookupExecutionContext) is only applicable forTracking, forSearch, forView, forAdvancedSearch, forSingleResult");
        }
    }

    @Override
    public String getValueTableName(final ExecutionContext theExecutionContext) {
        return "ETK_ROLE";
    }

    @Override
    public String getValueColumnName(final ExecutionContext theExecutionContext) {
        return "ROLE_ID";
    }
}
