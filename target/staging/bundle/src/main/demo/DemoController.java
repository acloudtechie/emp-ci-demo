/**
 *
 * DemoController
 *
 * administrator 07/16/2017
 **/

package demo;

import com.entellitrak.ApplicationException;
import com.entellitrak.user.User;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;
import com.entellitrak.PageExecutionContext;

public class DemoController implements PageController {

    public Response execute(PageExecutionContext etk) throws ApplicationException {
	User user = etk.getCurrentUser();
	    TextResponse response = etk.createTextResponse();
            response.put("user", user); return response;

    }

}