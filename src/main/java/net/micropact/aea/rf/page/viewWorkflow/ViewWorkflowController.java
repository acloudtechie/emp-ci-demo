package net.micropact.aea.rf.page.viewWorkflow;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.utility.StringEscapeUtils;

/**
 * This page controller is for providing a handy interface for viewing an RF Workflow in a graphical format.
 * It relies on other pages in order to work.
 * @author zmiller
 * @see net.micropact.aea.rf.page.viewWorkflowGraph.ViewWorkflowGraphController
 * @see net.micropact.aea.rf.page.viewWorkflowAjaxHandler.ViewWorkflowAjaxHandlerController
 * @see net.micropact.aea.rf.page.viewWorkflowGraphJavascript.ViewWorkflowGraphJavascriptController
 */
public class ViewWorkflowController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {

        final TextResponse response = etk.createTextResponse();

        final String workflowId = etk.getParameters().getSingle("rfWorkflowId");

        final String rfStates = etk.createSQL("SELECT ID, C_NAME FROM t_rf_state WHERE id_parent = :workflowId ORDER BY c_name")
                .setParameter("workflowId", workflowId)
                .fetchJSON();

        final String rfTransitions = etk.createSQL("SELECT rfTransition.id TRANSITIONID, rfTransition.c_name TRANSITIONNAME, rfTransition.c_code TRANSITIONCODE, transitionFromState.c_from_state FROMSTATE, rfTransition.c_to_state TOSTATE FROM t_rf_transition rfTransition LEFT JOIN m_rf_transition_from_state transitionFromState ON transitionFromState.id_owner = rfTransition.id WHERE rfTransition.id_parent = :workflowId ORDER BY c_name")
                .setParameter("workflowId", workflowId)
                .fetchJSON();

        final String rfTransitionEffects = etk.createSQL("SELECT effectTransition.id ID, effectTransition.C_TRANSITION, workflowEffect.C_EXECUTION_ORDER, workflowEffect.C_NAME FROM t_rf_workflow_effect workflowEffect JOIN m_rf_effect_transition effectTransition ON effectTransition.id_owner = workflowEffect.id WHERE workflowEffect.id_parent = :workflowId ORDER BY effectTransition.id")
                .setParameter("workflowId", workflowId)
                .fetchJSON();

        response.put("esc", StringEscapeUtils.class);

        response.put("workflowInfo", etk.createSQL("SELECT C_CODE FROM t_rf_workflow WHERE id = :rfWorkflowId").setParameter("rfWorkflowId", workflowId).fetchList().get(0));
        response.put("rfStates", rfStates);
        response.put("rfTransitions", rfTransitions);
        response.put("rfTransitionEffects", rfTransitionEffects);
        response.put("rfWorkflowId", workflowId);

        return response;
    }
}
