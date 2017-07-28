package net.micropact.aea.rf.page.viewWorkflowGraphJavascript;

import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.rf.utility.RfUtility;
import net.micropact.aea.utility.JsonUtilities;

/**
 * This is the controller page for the javascript of for rf.page.viewWorkflow.graph and is only intended to be
 * used with that page.
 * This page is actually responsible for creating the visual graph.
 * @author zmiller
 */
public class ViewWorkflowGraphJavascriptController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();

            RfUtility.cleanRfWorkflow(etk);

            final String workflowId = etk.getParameters().getSingle("rfWorkflowId");

            final String rfStates = etk.createSQL("SELECT id ID, c_name C_NAME, c_code C_CODE, c_x_coordinate C_X_COORDINATE, c_y_coordinate C_Y_COORDINATE FROM t_rf_state WHERE id_parent = :workflowId ORDER BY c_name")
                    .setParameter("workflowId", workflowId)
                    .fetchJSON();

            final String rfTransitions = etk.createSQL("SELECT rfTransition.id TRANSITIONID, rfTransition.c_code TRANSITIONCODE, rfTransition.c_name TRANSITIONNAME, transitionFromState.c_from_state FROMSTATE, rfTransition.c_to_state TOSTATE, rfTransition.c_initial_transition C_INITIAL_TRANSITION FROM t_rf_transition rfTransition LEFT JOIN m_rf_transition_from_state transitionFromState ON transitionFromState.id_owner = rfTransition.id WHERE rfTransition.id_parent = :workflowId ORDER BY c_name")
                    .setParameter("workflowId", workflowId)
                    .fetchJSON();

            final String rfTransitionEffects = etk.createSQL("SELECT effectTransition.id ID, effectTransition.c_transition C_TRANSITION, workflowEffect.c_execution_order C_EXECUTION_ORDER, workflowEffect.c_name C_NAME FROM t_rf_workflow_effect workflowEffect JOIN m_rf_effect_transition effectTransition ON effectTransition.id_owner = workflowEffect.id WHERE workflowEffect.id_parent = :workflowId ORDER BY workflowEffect.c_execution_order, effectTransition.id")
                    .setParameter("workflowId", workflowId)
                    .fetchJSON();

            final Map<String, Object> rfWorkflow = etk.createSQL("SELECT C_START_STATE_X_COORDINATE, C_START_STATE_Y_COORDINATE FROM t_rf_workflow WHERE id = :workflowId")
                    .setParameter("workflowId", workflowId)
                    .fetchMap();

            response.put("rfStates", rfStates);
            response.put("rfTransitions", rfTransitions);
            response.put("rfTransitionEffects", rfTransitionEffects);
            response.put("rfWorkflow", JsonUtilities.encode(rfWorkflow));

            return response;
        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }
}
