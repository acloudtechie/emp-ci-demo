package net.micropact.aea.rf.page.rfScriptParameterJavascript;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

/**
 * This page contains the javascript for the RF Script Parameter form.
 * @author zmiller
 */
public class RfScriptParameterJavascriptController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {

            final TextResponse response = etk.createTextResponse();

            response.put("parameterTypes", etk.createSQL("SELECT ID, C_CODE FROM t_rf_parameter_type ORDER BY c_code").fetchJSON());

            response.setContentType(ContentType.JAVASCRIPT);
            return response;
    }
}
