package net.micropact.aea.eu.page.euQueueAddressAjax;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.JsonUtilities;

/**
 * Ajax page for loading data needed on the EuQueueAddress form.
 *
 * @author Zachary.Miller
 */
public class EuQueueAddressAjaxController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        final TextResponse response = etk.createTextResponse();

        final String euQueueAddressId = etk.getParameters().getSingle("euQueueAddressId");

        response.setContentType(ContentType.JSON);

        response.put("out",
                JsonUtilities.encode(etk.createSQL("SELECT id EMAIL_ID, c_subject SUBJECT FROM t_eu_email_queue WHERE c_from_address = :queueAddressId ORDER BY c_subject, id")
                        .setParameter("queueAddressId", euQueueAddressId)
                        .fetchList()));

        return response;
    }
}
