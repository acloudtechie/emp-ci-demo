package net.micropact.aea.rf.page.manageRoleTransitions;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.Utility;

/**
 * <p>
 *  This page can be used to change the Roles which are allowed to take certain Transitions.
 * </p>
 * <p>
 *  The main use case for this page is that when new Roles are added,
 *  we don't want people to have to open up every single Transition to update that Role.
 * </p>
 *
 * @author zmiller
 */
public class ManageRoleTransitionsController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {
        final TextResponse response = etk.createTextResponse();

        /* The way we expect the transition-role data to come in is through the transitionRoles parameters
         * with values like <transitionId>_<roleId> for instance 3211_54121.*/
        final String rfWorkflowId = etk.getParameters().getSingle("rfWorkflowId");
        final String formAction = Optional.ofNullable(etk.getParameters().getSingle("formAction")).orElse("initial");
        final List<String> submittedTransitionRoles = Optional.ofNullable(etk.getParameters().getField("transitionRoles")).orElse(new LinkedList<>());

        if("commit".equals(formAction)){
            etk.createSQL("DELETE FROM m_rf_transition_role WHERE EXISTS(SELECT * FROM t_rf_transition rfTransition WHERE rfTransition.id_parent = :rfWorkflowId AND rfTransition.id = m_rf_transition_role.id_owner)")
            .setParameter("rfWorkflowId", rfWorkflowId)
            .execute();

            for(final String transitionRole : submittedTransitionRoles){
                final String[] transitionRoleArray = transitionRole.split("\\_");

                etk.createSQL(Utility.isSqlServer(etk) ? "INSERT INTO m_rf_transition_role (id_owner, list_order, c_role) VALUES(:transitionId, NULL, :roleId)"
                                                         : "INSERT INTO m_rf_transition_role (id, id_owner, list_order, c_role) VALUES(OBJECT_ID.NEXTVAL, :transitionId, NULL, :roleId)")
                .setParameter("transitionId", transitionRoleArray[0])
                .setParameter("roleId", transitionRoleArray[1])
                .execute();
            }
        }

        response.put("roles", etk.createSQL("SELECT role_id ROLEID, name NAME FROM etk_role ORDER BY name")
                .fetchJSON());

        response.put("transitions", etk.createSQL("SELECT rfTransition.id ID, rfTransition.c_name NAME, rfTransition.c_code CODE FROM t_rf_transition rfTransition WHERE rfTransition.id_parent = :rfWorkflowId ORDER BY c_order, name, code")
                .setParameter("rfWorkflowId", rfWorkflowId)
                .fetchJSON());

        response.put("roleTransitions", etk.createSQL("SELECT rfTransitionRole.id_owner RFTRANSITIONID, rfTransitionRole.c_role ROLEID FROM t_rf_transition rfTransition JOIN m_rf_transition_role rfTransitionRole ON rfTransitionRole.id_owner = rfTransition.id WHERE rfTransition.id_parent = :rfWorkflowId ORDER BY ROLEID, RFTRANSITIONID")
                .setParameter("rfWorkflowId", rfWorkflowId)
                .fetchJSON());

        return response;
    }
}
