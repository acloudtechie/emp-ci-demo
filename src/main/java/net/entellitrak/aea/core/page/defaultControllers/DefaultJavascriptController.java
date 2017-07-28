package net.entellitrak.aea.core.page.defaultControllers;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.pageUtility.PageUtility;

/**
 * This handler implements the bare minimum for a handler to be used in javascript view code.
 * @author zmiller
 */
public final class DefaultJavascriptController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {

        final TextResponse response = etk.createTextResponse();

        response.setContentType(ContentType.JAVASCRIPT);

        PageUtility.setAEACacheHeaders(etk, response);

        return response;
    }
}
