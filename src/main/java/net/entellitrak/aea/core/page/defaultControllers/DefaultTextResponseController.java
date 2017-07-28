package net.entellitrak.aea.core.page.defaultControllers;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.pageUtility.PageUtility;

/**
 * This page implements the bare minimum for a {@link com.entellitrak.page.TextResponse} as it
 * is common to have view code which does not depend on the controller code at all.
 * @author zmiller
 */
public final class DefaultTextResponseController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {

        final TextResponse response = etk.createTextResponse();

        PageUtility.setAEACacheHeaders(etk, response);

        return response;
    }
}
