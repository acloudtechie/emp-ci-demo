package net.micropact.aea.rf.page.ajaxGetRfScript;

import java.util.Collections;

import com.entellitrak.ApplicationException;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.JsonUtilities;

/**
 * Controller code for an ajax page which will return information about a particular RF Script object.
 *
 * @author zmiller
 */
public class AjaxGetRfScriptController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {

            final TextResponse response = etk.createTextResponse();

            final String rfScriptId = etk.getParameters().getSingle("rfScriptId");

            response.setContentType(ContentType.JSON);

            response.put("out", JsonUtilities.encode(etk.createSQL("SELECT ID, C_DESCRIPTION FROM t_rf_script WHERE id = :rfScriptId")
                    .setParameter("rfScriptId", rfScriptId)
                    .returnEmptyResultSetAs(Collections.emptyMap())
                    .fetchMap()));

            return response;
        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }
}
