package net.micropact.aea.rf.page.rfWorkflowParameterJavascript;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

/**
 * Entry point for all javascript on the RF Workflow Parameter form.
 *
 * @author zmiller
 */
public class RfWorkflowParameterJavascriptController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        final TextResponse response = etk.createTextResponse();

        response.put("parameterTypes", etk.createSQL("SELECT ID, C_CODE FROM t_rf_parameter_type ORDER BY c_code").fetchJSON());

        return response;
    }
}
