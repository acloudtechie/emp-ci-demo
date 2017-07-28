package net.micropact.aea.rf.page.ajaxGetEffectsUsingRfScript;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.query.Coersion;

/**
 * This class serves as the page controller for a page which can fetch the RF Workflow Effects which are associated
 * with a particular RF Script.
 *
 * @author Zachary.Miller
 */
public class AjaxGetEffectsUsingRfScript implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        final String rfScriptId = Coersion.toNonEmptyString(etk.getParameters().getSingle("rfScriptId"));

        final TextResponse response = etk.createTextResponse();
        response.setContentType(ContentType.JSON);

        response.put("out", etk.createSQL("SELECT rfWorkflow.c_code WORKFLOW_CODE, rfWorkflow.c_name WORKFLOW_NAME, rfWorkflowEffect.id EFFECT_ID, rfWorkflowEffect.c_name EFFECT_NAME, rfWorkflowEffect.c_code EFFECT_CODE FROM t_rf_workflow rfWorkflow JOIN t_rf_workflow_effect rfWorkflowEffect ON rfWorkflowEffect.id_parent = rfWorkflow.id WHERE rfWorkflowEffect.c_script = :rfScriptId ORDER BY WORKFLOW_NAME, WORKFLOW_CODE, EFFECT_NAME, EFFECT_CODE, EFFECT_ID")
                .setParameter("rfScriptId", rfScriptId)
                .fetchJSON());

        return response;
    }
}
