/**
 *
 * Controller for main JavaScript to be run on the Document form
 *
 * administrator 09/29/2016
 **/

package com.mptraining.refapp.complaint.page;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

public class DocumentJavaScriptController implements PageController {

    @Override
    public Response execute(PageExecutionContext etk) throws ApplicationException {

        TextResponse response = etk.createTextResponse();
        response.setContentType(ContentType.JAVASCRIPT);
        return response;

    }

}
