/**
 *
 * Controller for Main JavaScript to run on the Complaint form
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

public class ComplaintJavaScriptController implements PageController {

    @Override
	public Response execute(PageExecutionContext etk) throws ApplicationException {

        TextResponse response = etk.createTextResponse();
        response.setContentType(ContentType.JAVASCRIPT);

        response.put("role", etk.getCurrentUser().getRole().getBusinessKey());

        return response;

    }

}
