package net.micropact.aea.rf.page.ajaxGetRfParameterTypes;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

/**
 * Controller code for a page which fetches the RfParameterType objects.
 *
 * @author Zachary.Miller
 */
public class AjaxGetRfParameterTypes implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        final TextResponse response = etk.createTextResponse();

        response.setContentType(ContentType.JSON);

        response.put("out", etk.createSQL("SELECT ID, C_CODE FROM t_rf_parameter_type ORDER BY c_code").fetchJSON());

        return response;
    }
}
