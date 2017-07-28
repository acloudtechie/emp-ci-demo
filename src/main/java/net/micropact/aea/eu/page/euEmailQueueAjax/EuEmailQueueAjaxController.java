package net.micropact.aea.eu.page.euEmailQueueAjax;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

/**
 * This page controller returns the attachment information for a particular Email Queue entry.
 * Its original purpose is to retrieve this information for the EU Email Queue form javascript.
 *
 * @author zmiller
 */
public class EuEmailQueueAjaxController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
            final TextResponse response = etk.createTextResponse();
            response.setContentType(ContentType.JSON);

            final String emailQueueId = etk.getParameters().getSingle("emailQueueId");
            response.put("out", etk.createSQL("SELECT attachment.id ID, attachment.c_file C_FILE, f.file_name FILE_NAME FROM t_eu_queue_attachment attachment LEFT JOIN etk_file f ON f.id = attachment.c_file WHERE attachment.c_email_queue_id = :emailQueueId ORDER BY attachment.id")
                    .setParameter("emailQueueId", emailQueueId)
                    .fetchJSON());

            return response;
        }
}
