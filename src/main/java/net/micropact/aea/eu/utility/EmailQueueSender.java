package net.micropact.aea.eu.utility;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import com.entellitrak.ApplicationException;
import com.entellitrak.DataAccessException;
import com.entellitrak.ExecutionContext;

import net.entellitrak.aea.eu.AttachmentFactory;
import net.entellitrak.aea.eu.EmailQueue;
import net.entellitrak.aea.eu.IAttachment;
import net.entellitrak.aea.eu.SimpleEmail;
import net.entellitrak.aea.exception.EmailException;
import net.micropact.aea.core.cache.AeaCoreConfiguration;
import net.micropact.aea.core.exceptionTools.ExceptionUtility;
import net.micropact.aea.utility.Utility;

/**
 * This class provides a common location for code related to sending items in the EU Email Queue since there
 * is more than one place which does this, and the logic is a little involved and changes frequently.
 *
 * @author zmiller
 */
public final class EmailQueueSender {

    /** Hide the default constructor for Utility classes. */
    private EmailQueueSender(){}

    /**
     * Updates the EU Email Queue reference data list when an error is encountered.
     *
     * @param etk entellitrak execution context.
     * @param emailQueueId EU Email Queue tracking id.
     * @param exception description of the error.
     */
    private static void logError(final ExecutionContext etk, final String emailQueueId, final Exception exception){
        final String formattedMessage = ExceptionUtility.convertStackTraceToString(exception);
        etk.getLogger().error(String.format("Error sending email with emailQueueId \"%s\"\n%s",
                emailQueueId,
                formattedMessage));
        try {
            etk.createSQL("UPDATE t_eu_email_queue SET c_error = :error, c_status = (SELECT eqs.id FROM t_eu_email_queue_status eqs WHERE eqs.c_code = 'error') WHERE id = :emailQueueId")
            .setParameter("error", formattedMessage)
            .setParameter("emailQueueId", emailQueueId)
            .execute();
        } catch (final DataAccessException e) {
            etk.getLogger().error(e);
        }
    }

    /**
     * This method attempts to send emails in the Email Queue. Which emails it attempts to send is partially controlled
     * by the AEA - CORE - Configuration RDO. For instance it will stop attempting to send emails  with status error
     * which are more than a certain age.
     *
     * @param etk entellitrak Execution Context
     * @param onlySendErrors This indicates what type of emails we want to send.
     *          false means that we want to send any unsent emails that were created relatively recently.
     *          true means that we should try to resend ALL error emails regardless of how long ago they were created.
     * @throws EmailException If any errors were encountered
     */
    public static void sendQueuedEmails(final ExecutionContext etk, final boolean onlySendErrors) throws EmailException{

        /* Get the emails that need to be sent */
        List<Map<String, Object>> emailQueues; /*EMAILQUEUEID, SUBJECT, BODY, RECIPIENTS, CCRECIPIENTS, BCCRECIPIENTS*/
        try {
            if(onlySendErrors){
                emailQueues = etk.createSQL(Utility.isSqlServer(etk) ? "SELECT eq.id EMAILQUEUEID, eq.c_subject SUBJECT, eq.c_body BODY, eq.c_recipients RECIPIENTS, eq.c_cc_recipients CCRECIPIENTS, eq.c_bcc_recipients BCCRECIPIENTS, fromAddress.id FROMID, fromAddress.c_address FROMADDRESS, fromAddress.c_personal FROMPERSONAL FROM t_eu_email_queue eq WITH ( UPDLOCK ) JOIN t_eu_email_queue_status eqs ON eqs.id = eq.c_status LEFT JOIN t_eu_queue_address fromAddress ON fromAddress.id = eq.c_from_address WHERE eqs.c_code = 'error' ORDER BY eq.c_created_time"
                        : "SELECT eq.id EMAILQUEUEID, eq.c_subject SUBJECT, eq.c_body BODY, eq.c_recipients RECIPIENTS, eq.c_cc_recipients CCRECIPIENTS, eq.c_bcc_recipients BCCRECIPIENTS, fromAddress.id FROMID, fromAddress.c_address FROMADDRESS, fromAddress.c_personal FROMPERSONAL FROM t_eu_email_queue eq JOIN t_eu_email_queue_status eqs ON eqs.id = eq.c_status LEFT JOIN t_eu_queue_address fromAddress ON fromAddress.id = eq.c_from_address WHERE eqs.c_code = 'error' ORDER BY eq.c_created_time FOR UPDATE ")
                        .fetchList();
            }else{
                emailQueues = etk.createSQL(Utility.isSqlServer(etk) ? "SELECT eq.id EMAILQUEUEID, eq.c_subject SUBJECT, eq.c_body BODY, eq.c_recipients RECIPIENTS, eq.c_cc_recipients CCRECIPIENTS, eq.c_bcc_recipients BCCRECIPIENTS, fromAddress.id FROMID, fromAddress.c_address FROMADDRESS, fromAddress.c_personal FROMPERSONAL FROM t_eu_email_queue eq WITH ( UPDLOCK ) JOIN t_eu_email_queue_status eqs ON eqs.id = eq.c_status LEFT JOIN t_eu_queue_address fromAddress ON fromAddress.id = eq.c_from_address WHERE eqs.c_code = 'created' OR (eqs.c_code = 'error' AND (:minutesUntilAbortResending IS NULL OR DATEDIFF(MINUTE, c_created_time, DBO.ETKF_GETSERVERTIME()) <= :minutesUntilAbortResending)) ORDER BY eq.c_created_time "
                        : "SELECT eq.id EMAILQUEUEID, eq.c_subject SUBJECT, eq.c_body BODY, eq.c_recipients RECIPIENTS, eq.c_cc_recipients CCRECIPIENTS, eq.c_bcc_recipients BCCRECIPIENTS, fromAddress.id FROMID, fromAddress.c_address FROMADDRESS, fromAddress.c_personal FROMPERSONAL FROM t_eu_email_queue eq JOIN t_eu_email_queue_status eqs ON eqs.id = eq.c_status LEFT JOIN t_eu_queue_address fromAddress ON fromAddress.id = eq.c_from_address WHERE eqs.c_code = 'created' OR (eqs.c_code = 'error' AND (:minutesUntilAbortResending IS NULL OR eq.c_created_time >= ETKF_GETSERVERTIME() - :minutesUntilAbortResending * 1 / 24 / 60) ) ORDER BY eq.c_created_time FOR UPDATE")
                        .setParameter("minutesUntilAbortResending",
                                AeaCoreConfiguration.getEuMinutesUntilAbortResendingErrors(etk))
                        .fetchList();
            }
        } catch (final DataAccessException | ApplicationException e) {
            throw new EmailException(e);
        }

        for(final Map<String, Object> emailQueue : emailQueues){
            try{
                final List<Map<String, Object>> emailQueueAttachments = etk.createSQL("SELECT C_FILE FROM t_eu_queue_attachment WHERE c_email_queue_id = :emailQueueId ORDER BY id")
                        .setParameter("emailQueueId",
                                emailQueue.get("EMAILQUEUEID")).fetchList();

                final Collection<IAttachment> attachments = new LinkedList<>();

                for(final Map<String, Object> attachment : emailQueueAttachments){
                    attachments.add(AttachmentFactory.generateAttachment(etk,
                            ((Number) attachment.get("C_FILE")).longValue()));
                }

                final InternetAddress fromAddress;
                final Object fromId = emailQueue.get("FROMID");
                if(fromId == null){
                    fromAddress = null;
                }else{
                    fromAddress = new InternetAddress(
                            (String) emailQueue.get("FROMADDRESS"),
                            (String) emailQueue.get("FROMPERSONAL"));
                }

                EmailQueue.sendEmail(etk, new SimpleEmail(etk,
                        (String) emailQueue.get("SUBJECT"),
                        (String) emailQueue.get("BODY"),
                        fromAddress,
                        emailQueue.get("RECIPIENTS"),
                        emailQueue.get("CCRECIPIENTS"),
                        emailQueue.get("BCCRECIPIENTS"),
                        attachments));

                etk.createSQL(Utility.isSqlServer(etk) ? "UPDATE t_eu_email_queue SET c_sent_time = DBO.ETKF_GETSERVERTIME(), c_error = NULL, c_status = ( SELECT eqs.id FROM t_eu_email_queue_status eqs WHERE eqs.c_code = 'sent' ) WHERE id = :emailQueueId"
                        : "UPDATE t_eu_email_queue SET c_sent_time = ETKF_GETSERVERTIME(), c_error = NULL, c_status = (SELECT eqs.id FROM t_eu_email_queue_status eqs WHERE eqs.c_code = 'sent') WHERE id = :emailQueueId ")
                    .setParameter("emailQueueId", emailQueue.get("EMAILQUEUEID")).execute();

            } catch (final Exception exception){
                logError(etk, emailQueue.get("EMAILQUEUEID").toString(), exception);
            }
        }
    }
}
