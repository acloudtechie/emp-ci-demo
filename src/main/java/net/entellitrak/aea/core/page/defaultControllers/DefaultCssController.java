package net.entellitrak.aea.core.page.defaultControllers;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.pageUtility.PageUtility;

/**
 * This controller is for use in static CSS pages which do not need any information passed through the context.
 * @author zmiller
 */
public final class DefaultCssController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {

        final TextResponse response = etk.createTextResponse();

        response.setContentType(ContentType.CSS);

        PageUtility.setAEACacheHeaders(etk, response);

        return response;
    }
}
