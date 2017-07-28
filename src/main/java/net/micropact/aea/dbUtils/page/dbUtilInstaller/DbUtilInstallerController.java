/**
 *
 * DbUtilInstallerController
 *
 * administrator 09/22/2014
 **/

package net.micropact.aea.dbUtils.page.dbUtilInstaller;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.entellitrak.aea.core.CoreServiceFactory;

/**
 * This class creates a set of common views, procedures and functions that will shorten the amount of SQL AEs need to
 * write and simplify the AEA library SQL calls.
 *
 * @author aclee
 *
 */
public class DbUtilInstallerController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        final TextResponse response = etk.createTextResponse();
        response.setContentType("text/plain");

        response.put("out", CoreServiceFactory
                .getDeploymentService(etk)
                .runComponentSetup()
                .getSummaryString());
        return response;
    }
}
