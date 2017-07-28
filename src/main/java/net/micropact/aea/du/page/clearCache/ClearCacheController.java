/**
 *
 * Cache clearing utility controller.
 *
 * alee 11/03/2014
 **/

package net.micropact.aea.du.page.clearCache;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

/**
 * Short helper utility to immediately call etk.getCache().clearCache().
 *
 * @author aclee
 *
 */
public class ClearCacheController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {

        final TextResponse response = etk.createTextResponse();

        etk.getCache().clearCache();
        etk.getDataCacheService().clearDataCaches();

        return response;
    }
}
