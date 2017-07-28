package net.micropact.aea.du.page.cacheRemoveEntryAjax;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

/**
 * This is the controller code for a page which removes a particular entry from the {@link com.entellitrak.cache.Cache}.
 *
 * @author zmiller
 */
public class CacheRemoveEntryAjaxController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {

        final TextResponse response = etk.createTextResponse();
        response.setContentType(ContentType.JSON);

        final String cacheKey = etk.getParameters().getSingle("cacheKey");
        etk.getCache().remove(cacheKey);

        response.put("out", "{\"status\": \"success\"}");

        return response;
    }
}
