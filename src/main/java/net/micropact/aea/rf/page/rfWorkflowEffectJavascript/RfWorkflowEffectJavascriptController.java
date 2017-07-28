package net.micropact.aea.rf.page.rfWorkflowEffectJavascript;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.pageUtility.PageUtility;
import net.micropact.aea.rf.utility.dynamicParameters.DynamicParametersUseFactory.DynamicParameterUsage;
import net.micropact.aea.utility.JsonUtilities;

/**
 * Controller code for the RF Workflow Effect Javascript page.
 *
 * @author zmiller
 */
public class RfWorkflowEffectJavascriptController implements PageController{

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        final TextResponse response = etk.createTextResponse();

        response.setContentType(ContentType.JAVASCRIPT);

        PageUtility.setAEACacheHeaders(etk, response);

        response.put("dynamicParameterUsageType",
                JsonUtilities.encode(DynamicParameterUsage.RF_WORKFLOW_EFFECT_PARAMETER));

        return response;
    }
}
