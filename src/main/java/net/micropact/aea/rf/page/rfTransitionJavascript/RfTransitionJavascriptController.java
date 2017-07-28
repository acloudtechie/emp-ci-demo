package net.micropact.aea.rf.page.rfTransitionJavascript;

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
 * Controller code for the RF Transition - Javascript page..
 *
 * @author zmiller
 */
public class RfTransitionJavascriptController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        final TextResponse response = etk.createTextResponse();

        response.setContentType(ContentType.JAVASCRIPT);

        PageUtility.setAEACacheHeaders(etk, response);

        response.put("dynamicParameterUsageType",
                JsonUtilities.encode(DynamicParameterUsage.RF_TRANSITION_PARAMETER));

        return response;
    }
}
