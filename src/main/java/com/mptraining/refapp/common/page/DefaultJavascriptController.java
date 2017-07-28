/**
 *
 * This handler implements the bare minimum for a handler to be used in JavaScript view code.
 *
 * administrator 05/22/2017
 **/

package com.mptraining.refapp.common.page;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

public class DefaultJavascriptController implements PageController {

    public Response execute(PageExecutionContext etk) throws ApplicationException {

    	final TextResponse response = etk.createTextResponse();

        response.setContentType(ContentType.JAVASCRIPT);

        /* Set caching to expire in 1 day. This is long enough to make a huge difference, but short enough that if upgrades
         * happen during the weekend, that users will have more than enough time to get the new version when they
         * come in on Monday. */
        response.setHeader("Cache-Control", "public, max-age=86400");
        response.setHeader("Pragma", "");

        return response;

    }

}
