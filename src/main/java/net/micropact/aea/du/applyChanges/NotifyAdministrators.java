package net.micropact.aea.du.applyChanges;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.mail.Mail;
import com.entellitrak.mail.MailException;
import com.entellitrak.mail.MailService;
import com.entellitrak.system.ApplyChangesEventHandler;
import com.entellitrak.system.ApplyChangesExecutionContext;

import net.micropact.aea.core.utility.StringEscapeUtils;
import net.micropact.aea.utility.SchedulerJobUtility;
import net.micropact.aea.utility.SchedulerJobUtility.SchedulerJobError;
import net.micropact.aea.utility.Utility;

/**
 * This is an Apply Changes listener which will Email a Group (aea.du.applyChangesCompleteEmailRecipients) when an
 * Apply Changes finishes. The Email will contain information about the Apply Changes.
 *
 * @author zmiller
 */
public class NotifyAdministrators implements ApplyChangesEventHandler {

    private static final long MILLISECONDS_PER_SECOND  = 1000;
    private static final long SECONDS_PER_MINUTE = 60;
    private static final long MINUTES_PER_HOUR = 60;

    @Override
    public void execute(final ApplyChangesExecutionContext etk) throws ApplicationException {
        try {
            final Collection<String> recipients = getRecipients(etk);

            /* The new mail API throws exceptions if recipients are empty so we only send the email if there are
             * recipients. */
            if(!recipients.isEmpty()){
                final boolean operationSuccessful = etk.isOperationSucceeded();
                final String emailSubject = getEmailSubject(operationSuccessful);
                final String emailBody = getEmailBody(etk);

                final MailService mailService = etk.getMailService();

                final Mail mail = mailService.createMail();
                mail.setHtmlMessage(true);
                mail.setSubject(emailSubject);
                mail.setMessage(emailBody);
                recipients.forEach(mail::addTo);

                try {
                    mailService.send(mail);
                } catch (final MailException e) {
                    Utility.aeaLog(etk,
                            "An error was encountered attempting to send the Apply Changes notification email",
                            e);
                }
            }
        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Generates a list of recipients who should get the Apply Changes Email. Nulls/Blanks will be filtered out.
     *
     * @param etk entellitrak execution context
     * @return The Email Address of the Email Recipients
     */
    private static Collection<String> getRecipients(final ExecutionContext etk){
        return etk.createSQL("SELECT u.EMAIL_ADDRESS FROM etk_group g JOIN etk_user_group_assoc uga ON uga.group_id = g.group_id JOIN etk_user u ON u.user_id = uga.user_id WHERE g.business_key = :groupBusinessKey AND u.type_of_user = 1")
                .setParameter("groupBusinessKey", "aea.du.applyChangesCompleteEmailRecipients")
                .fetchList()
                .stream()
                .map(user -> (String) user.get("EMAIL_ADDRESS"))
                .filter(emailAddress -> !Utility.isBlank(emailAddress))
                .collect(Collectors.toList());
    }

    /**
     * Generates an email subject which indicates whether an Apply Changes was successful or not.
     *
     * @param operationSuccessful If the Apply Changes was successful
     * @return The Email subject
     */
    private static String getEmailSubject(final boolean operationSuccessful){
        if(operationSuccessful){
            return "Apply Changes completed Successfully";
        }else{
            return "Apply Changes encountered an Error";
        }
    }


    /**
     * Generates the body of the email which will be sent and includes information about the Update Log and
     * Scheduler Jobs.
     *
     * @param etk entellitrak execution context
     * @return The entire Email Body
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static String getEmailBody(final ExecutionContext etk) throws IncorrectResultSizeDataAccessException{
        return String.format("%s%s<br/><br/>%s",
                getStyle(),
                getUpdateLogInfoSection(etk),
                getSchedulerJobSection(etk));
    }

    /**
     * Generates the HTML style element which should be used in the email.
     *
     * @return An HTML representation of the CSS style element to be used in the email
     */
    private static Object getStyle() {
        // If you need to edit this String, I recommend using an online java string escaper/unescaper
        return "<style>\r\n  table{\r\n  \tborder-collapse: collapse;\r\n  }\r\n  \r\n  table td, table th{\r\n  \tborder: 1px solid black;\r\n  \tpadding: 0.1em 0.7em;\r\n  }\r\n  \r\n  .success{\r\n  \tcolor: #22bb22;\r\n  }\r\n  \r\n  .error{\r\n  \tcolor: #ff0000;\r\n  }\r\n</style>";
    }

    /**
     * Generates an HTML representation of the Scheduler Jobs which indicates whether or not they have errors.
     *
     * @param etk entellitrak execution context
     * @return The HTML String representation
     */
    private static String getSchedulerJobSection(final ExecutionContext etk) {
        final List<SchedulerJobError> schedulerJobErrors = SchedulerJobUtility.getLikelySchedulerJobErrors(etk);

        return String.format("Scheduler Job Status: %s%s",
                !schedulerJobErrors.isEmpty()
                ? "<span class=\"error\">ERROR</span>"
                  : "<span class=\"success\">GOOD</span>",
                  SchedulerJobUtility.convertErrorsToHTML(schedulerJobErrors, false));
    }

    /**
     * Gets an HTML String which contains information about the last Update Log entry.
     *
     * @param etk entellitrak execution context
     * @return An HTML representation of the update log information
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static String getUpdateLogInfoSection(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException {
        final Map<String, Object> updateLogInfo = etk.createSQL("SELECT updateLog.DESCRIPTION, updateLog.END_TIMESTAMP, updateLog.START_TIMESTAMP, u.USERNAME, updateLog.IP_ADDRESS FROM etk_update_log updateLog LEFT JOIN etk_user u ON u.user_id = updateLog.user_id WHERE update_log_id = (SELECT MAX(update_log_id) FROM etk_update_log)")
                .fetchMap();
        updateLogInfo.put("duration", formatMilliseconds(calculateDateDifference(
                (Date) updateLogInfo.get("START_TIMESTAMP"),
                (Date) updateLogInfo.get("END_TIMESTAMP"))));

        final String[][] headerKeys = new String[][]{
            {"Description", "DESCRIPTION"},
            {"Duration", "duration"},
            {"Start Time", "START_TIMESTAMP"},
            {"End Time", "END_TIMESTAMP"},
            {"Username", "USERNAME"},
            {"IP Address", "IP_ADDRESS"}};

            final StringBuilder tableHeaderBuilder = new StringBuilder();
            final StringBuilder tableRowBuilder = new StringBuilder();

            for(final String[] headerKey : headerKeys){
                tableHeaderBuilder.append(String.format("<th>%s</th>", StringEscapeUtils.escapeHtml(headerKey[0])));
                tableRowBuilder.append(String.format("<td>%s</td>",
                        StringEscapeUtils.escapeHtml(updateLogInfo.get(headerKey[1]).toString())));
            }

            return String.format("<table><thead><tr>%s</tr></thead><tbody><tr>%s</tr></tbody></table>",
                    tableHeaderBuilder.toString(),
                    tableRowBuilder.toString());
    }

    /**
     * Calculates the number of milliseconds between two Dates.
     *
     * @param startTime The start Date
     * @param endTime The end Date
     * @return The number of milliseconds between the two dates
     */
    private static long calculateDateDifference(final Date startTime, final Date endTime) {
        return endTime.getTime() - startTime.getTime();
    }

    /**
     * This converts a duration which is represented by milliseconds, to a text-formatted String such as
     * &quot;4 hr 3 min 20 sec&quot;.
     *
     * @param milliseconds milliseconds of the duration
     * @return A text formatted String representing the duration.
     */
    private static String formatMilliseconds(final long milliseconds) {

        final long totalSeconds = milliseconds / MILLISECONDS_PER_SECOND;
        final long seconds = totalSeconds % SECONDS_PER_MINUTE;
        final long totalMinutes = (totalSeconds - seconds) / SECONDS_PER_MINUTE;
        final long minutes = totalMinutes % MINUTES_PER_HOUR;
        final long hours = (totalMinutes - minutes) / MINUTES_PER_HOUR;

        return String.format("%s%s%s",
                hours > 0 ? hours + " hr " : "",
                          totalMinutes > 0 ? minutes + " min " : "",
                                           seconds + " sec").trim();
    }
}
