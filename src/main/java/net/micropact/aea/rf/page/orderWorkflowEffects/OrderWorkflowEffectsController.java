package net.micropact.aea.rf.page.orderWorkflowEffects;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

/**
 * This page can be used to easily view and change the order that RF Workflow Effects are fired in.
 * @author zmiller
 * @see net.micropact.aea.rf.page.orderWorkflowEffectsAjaxHandler.OrderWorkflowEffectsAjaxHandlerController
 */
public class OrderWorkflowEffectsController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {
        final TextResponse response = etk.createTextResponse();

        final String rfWorkflowId = etk.getParameters().getSingle("rfWorkflowId");

        response.put("rfWorkflow", etk.createSQL("SELECT rfWorkflow.ID, rfWorkflow.C_CODE FROM t_rf_workflow rfWorkflow WHERE rfWorkflow.id = :rfWorkflowId")
                .setParameter("rfWorkflowId", rfWorkflowId)
                .fetchJSON());
        response.put("workflowEffects", etk.createSQL("SELECT rfWorkflowEffect.C_NAME, rfWorkflowEffect.ID, rfWorkflowEffect.C_EXECUTION_ORDER FROM t_rf_workflow_effect rfWorkflowEffect WHERE rfWorkflowEffect.id_parent = :rfWorkflowId ORDER BY rfWorkflowEffect.c_execution_order")
                .setParameter("rfWorkflowId", rfWorkflowId)
                .fetchJSON());

        return response;
    }
}
