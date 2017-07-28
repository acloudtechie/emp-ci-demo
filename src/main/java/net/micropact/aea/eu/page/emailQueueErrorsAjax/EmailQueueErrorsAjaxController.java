package net.micropact.aea.eu.page.emailQueueErrorsAjax;

import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.localization.Localizations;
import com.entellitrak.localization.TimeZonePreferenceInfo;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.eu.utility.EmailQueueSender;
import net.micropact.aea.utility.JsonUtilities;

/**
 * This ajax page will return a list of email queue errors.
 * If the parameter sendEmails=1 is passed, then the page will attempt to send error emails before generating the list.
 * This page is intended to be embedded within eu.dashboard.emailQueueErrors
 *
 * @author zmiller
 */
public class EmailQueueErrorsAjaxController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {

        try {
            final boolean sendEmails = "1".equals(etk.getParameters().getSingle("sendEmails"));
            final TimeZonePreferenceInfo timezonePreference = etk.getCurrentUser().getTimeZonePreference();

            if(sendEmails){
                EmailQueueSender.sendQueuedEmails(etk, true);
            }

            final TextResponse response = etk.createTextResponse();
            response.setContentType(ContentType.JSON);

            final Stream<Map<String, Object>> emailQueues = etk.createSQL("SELECT emailQueue.id EMAILQUEUEID, emailQueue.c_created_time CREATEDTIME, emailQueue.c_recipients RECIPIENTS, emailQueue.c_cc_recipients CCRECIPIENTS, emailQueue.c_bcc_recipients BCCRECIPIENTS, emailQueue.c_subject SUBJECT, emailQueue.c_error ERROR, (SELECT COUNT(*) FROM t_eu_queue_attachment queueAttachment WHERE queueAttachment.c_email_queue_id = emailQueue.id) ATTACHMENTS FROM t_eu_email_queue emailQueue JOIN t_eu_email_queue_status emailQueueStatus ON emailQueueStatus.id = emailQueue.c_status WHERE emailQueueStatus.c_code = 'error' ORDER BY emailQueue.c_created_time")
                    .fetchList()
                    .stream()
                    .map(emailQueue -> {
                        emailQueue.put("CREATEDTIME",
                                Localizations.toLocalTimestamp(timezonePreference,
                                        (Date) emailQueue.get("CREATEDTIME"))
                                .getTimestampString());
                        return emailQueue;});

            response.put("out", JsonUtilities.encode(emailQueues));

            return response;
        } catch (final Exception e) {
            throw new ApplicationException(e);
        }
    }
}
