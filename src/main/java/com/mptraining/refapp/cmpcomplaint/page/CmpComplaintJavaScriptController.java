/**
 *
 * Controller for main JavaScript to run on the CMP Complaint form
 *
 * administrator 12/14/2016
 **/

package com.mptraining.refapp.cmpcomplaint.page;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

public class CmpComplaintJavaScriptController implements PageController {

    public Response execute(PageExecutionContext etk) throws ApplicationException {

    	TextResponse response = etk.createTextResponse();
        response.setContentType(ContentType.JAVASCRIPT);

        response.put("role", etk.getCurrentUser().getRole().getBusinessKey());

        return response;

    }

}
