package net.entellitrak.aea.eu;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.mail.Attachment;
import com.entellitrak.mail.Mail;

import net.entellitrak.aea.exception.EmailException;
import net.micropact.aea.core.cache.AeaCoreConfiguration;
import net.micropact.aea.core.enums.AeaCoreConfigurationItem;
import net.micropact.aea.core.ioUtility.IOUtility;
import net.micropact.aea.eu.utility.FileUtility;
import net.micropact.aea.eu.utility.InternetAddressUtility;
import net.micropact.aea.utility.Utility;

/**
 * <p>
 *  This class is used to actually send emails.
 *  It has the ability to either send an email immediately or to put it in a queue to send later.
 *  The advantage of using the queue is that if the transaction is rolled back, the emails will not be sent.
 *  Also if an exception is encountered actually trying to send the email, then further attempts can be made later.
 * </p>
 *
 * @author zmiller
 */
public final class EmailQueue {

    /**
     * There is no reason to instantiate an EmailQueue.
     */
    private EmailQueue(){}

    /**
     * This function will put an email into a reference data table to be sent out later.
     *
     * @param etk The etk variable in entellitrak
     * @param email The email to send
     * @return The id of the new entry in T_EU_EMAIL_QUEUE
     * @throws EmailException If any problem occurs
     */
    public static long queueEmail(final ExecutionContext etk, final IEmail email) throws EmailException{
        try {
            final long emailQueueId;
            final Long fromAddressId;

            /* Handle from address */
            final InternetAddress fromAddress = email.getFrom();
            if(fromAddress == null){
                fromAddressId = null;
            }else{
                final Map<String, Object> addressParameters = Utility.arrayToMap(String.class, Object.class, new Object[][]{
                    {"address", fromAddress.getAddress()},
                    {"personal", fromAddress.getPersonal()}
                });

                if(Utility.isSqlServer(etk)){
                    fromAddressId = ((Number) etk.createSQL("INSERT INTO t_eu_queue_address (c_address, c_personal) VALUES(:address, :personal)")
                            .setParameter(addressParameters)
                            .executeForKey("id")).longValue();
                }else{
                    fromAddressId = ((Number) etk.createSQL("SELECT OBJECT_ID.NEXTVAL FROM DUAL").fetchObject()).longValue();

                    etk.createSQL("INSERT INTO t_eu_queue_address (id, c_address, c_personal) VALUES(:fromAddressId, :address, :personal)")
                    .setParameter(addressParameters)
                    .setParameter("fromAddressId", fromAddressId)
                    .execute();
                }
            }

            /* Handle main email */
            final Map<String, Object> queueParameters = Utility.arrayToMap(String.class, Object.class, new Object[][]{
                {"bccRecipients", InternetAddressUtility.toText(email.getBccRecipients())},
                {"body", email.getBody()},
                {"ccRecipients", InternetAddressUtility.toText(email.getCcRecipients())},
                {"fromAddress", fromAddressId},
                {"recipients", InternetAddressUtility.toText(email.getRecipients())},
                {"subject", email.getSubject()}
            });
            if(Utility.isSqlServer(etk)){
                emailQueueId = ((Number)
                        etk.createSQL("INSERT INTO t_eu_email_queue(c_bcc_recipients, c_body, c_cc_recipients, c_from_address, c_recipients, c_subject, c_created_time, c_sent_time, c_status) VALUES(:bccRecipients, :body, :ccRecipients, :fromAddress, :recipients, :subject, DBO.ETKF_GETSERVERTIME(), NULL, (SELECT eqs.id FROM t_eu_email_queue_status eqs WHERE eqs.c_code = 'created'))")
                        .setParameter(queueParameters)
                        .executeForKey("id")).longValue();
            }else{
                emailQueueId = ((Number) etk.createSQL("SELECT OBJECT_ID.NEXTVAL FROM DUAL").fetchObject()).longValue();

                etk.createSQL("INSERT INTO t_eu_email_queue(id, c_bcc_recipients, c_body, c_cc_recipients, c_from_address, c_recipients, c_subject, c_created_time, c_sent_time, c_status) VALUES(:emailQueueId, :bccRecipients, :body, :ccRecipients, :fromAddress, :recipients, :subject, ETKF_GETSERVERTIME(), NULL, (SELECT eqs.id FROM t_eu_email_queue_status eqs WHERE eqs.c_code = 'created'))")
                .setParameter(queueParameters)
                .setParameter("emailQueueId", emailQueueId)
                .execute(); //.executeForKey("ID"); /*I don't know why i can't use executeForKey("ID")*/
            }

            /* Handle attachments */
            for(final IAttachment attachment : email.getAttachments()){
                try(final InputStream contentStream = attachment.getContent()){

                    final String attachmentName = attachment.getName();

                    final byte[] fileContent = IOUtility.toByteArray(contentStream);

                    final String fileExtension =
                            attachmentName.indexOf('.') == -1
                            ? ""
                              : attachmentName.substring(attachmentName.indexOf('.'));
                    String contentType = URLConnection.guessContentTypeFromName(attachmentName);
                    if(contentType == null){
                        contentType = "application/octet-stream";
                    }

                    if(Utility.isSqlServer(etk)){

                        final String emailQueueAttachmentId = String.valueOf(
                                etk.createSQL("INSERT INTO t_eu_queue_attachment(c_file, c_email_queue_id) VALUES(NULL, :emailQueueId)")
                                .setParameter("emailQueueId", emailQueueId)
                                .executeForKey("id"));

                        final String fileId = String.valueOf(
                                etk.createSQL("INSERT INTO etk_file(file_name, file_size, content_type, file_Type, file_extension, object_type, reference_id, content, resource_path) VALUES(:fileName, :fileSize, :contentType, :fileType, :fileExtension, :objectType, :referenceId, :content, NULL)")
                                .setParameter("fileName", attachmentName)
                                .setParameter("fileSize", fileContent.length)
                                .setParameter("contentType", contentType)
                                .setParameter("fileType", 1)
                                .setParameter("fileExtension", fileExtension)
                                .setParameter("objectType", "T_EU_QUEUE_ATTACHMENT")
                                .setParameter("referenceId", emailQueueAttachmentId)
                                .setParameter("content", fileContent)
                                .executeForKey("id"));

                        etk.createSQL("UPDATE t_eu_queue_attachment SET c_file = :fileId WHERE id = :attachmentId")
                        .setParameter("fileId", fileId)
                        .setParameter("attachmentId", emailQueueAttachmentId)
                        .execute();
                    }else{
                        final Map<String, Object> nextIds = etk.createSQL("SELECT OBJECT_ID.NEXTVAL fileId, OBJECT_ID.NEXTVAL attachmentId FROM DUAL")
                                .fetchList().get(0);

                        etk.createSQL("INSERT INTO t_eu_queue_attachment(id, c_file, c_email_queue_id) VALUES(:attachmentId, :fileId, :emailQueueId)")
                        .setParameter("attachmentId", nextIds.get("ATTACHMENTID"))
                        .setParameter("fileId", nextIds.get("FILEID"))
                        .setParameter("emailQueueId", emailQueueId)
                        .execute();
                        etk.createSQL("INSERT INTO etk_file(id, file_name, file_size, content_type, file_Type, file_extension, object_type, reference_id, content, resource_path) VALUES(:fileId, :fileName, :fileSize, :contentType, :fileType, :fileExtension, :objectType, :referenceId, :content, null)")
                        .setParameter("fileId", nextIds.get("FILEID"))
                        .setParameter("fileName", attachmentName)
                        .setParameter("fileSize", fileContent.length)
                        .setParameter("contentType", contentType)
                        .setParameter("fileType", 1)
                        .setParameter("fileExtension", fileExtension)
                        .setParameter("objectType", "T_EU_QUEUE_ATTACHMENT")
                        .setParameter("referenceId", nextIds.get("ATTACHMENTID"))
                        .setParameter("content", fileContent)
                        .execute();
                    }
                }
            }
            return emailQueueId;
        } catch (final IncorrectResultSizeDataAccessException | IOException e) {
            throw new EmailException("Error encountered queueing email", e);
        }
    }

    /**
     * Immediately sends an email.
     * NOTE: CURRENTLY DUE TO CORE LIMITATIONS, IF TWO FILES HAVE THE SAME NAME, I WILL CHANGE ONE OF THEM.
     *
     * @param etk The etk variable in entellitrak
     * @param email The email to send
     * @throws EmailException If any problem occurs
     */
    public static void sendEmail(final ExecutionContext etk, final IEmail email) throws EmailException{

        final String errorString = "Error encountered sending email.";

        try {
            if(AeaCoreConfiguration.isEuEmailEnabled(etk)){

                final Collection<String> recipients = InternetAddressUtility.toTextCollection(email.getRecipients());
                final Collection<String> ccRecipients =
                        InternetAddressUtility.toTextCollection(email.getCcRecipients());
                final Collection<String> bccRecipients =
                        InternetAddressUtility.toTextCollection(email.getBccRecipients());

                if(recipients.size() > 0
                        || ccRecipients.size() > 0
                        || bccRecipients.size() > 0){

                    final Mail mail = etk.getMailService().createMail();

                    for(final String recipient : recipients){
                        mail.addTo(recipient);
                    }

                    for(final String ccRecipient : ccRecipients){
                        mail.addCc(ccRecipient);
                    }

                    for(final String bccRecipient : bccRecipients){
                        mail.addBcc(bccRecipient);
                    }

                    mail.setSubject(email.getSubject());
                    mail.setMessage(email.getBody());
                    mail.setHtmlMessage(true);

                    final InternetAddress fromAddress = email.getFrom();
                    if(fromAddress != null){
                        mail.setFrom(etk.getMailService().createInternetAddress(
                                fromAddress.getAddress(),
                                fromAddress.getPersonal()));
                    }

                    for(final IAttachment attachment : email.getAttachments()){
                        try (InputStream contentStream = attachment.getContent()) {
                            mail.addAttachment(
                                    new Attachment(attachment.getName(),
                                    FileUtility.toByteArray(etk, contentStream)));
                        } catch (final Exception e) {
                            throw new EmailException(String.format("Error getting attachment with name: \"%s\"", attachment.getName()),
                                    e);
                        }
                    }

                    etk.getMailService().send(mail);
                }
            }else{
                etk.getLogger().error(
                        String.format("An email has not been sent to the email server because emails are not enabled. If you want emails sent you must set the \"%s\" option in the \"AEA CORE Configuration\" RDO.", AeaCoreConfigurationItem.EU_ENABLE_EMAIL.getCode()));
            }
        } catch (final Exception  e) {
            throw new EmailException(errorString, e);
        }
    }
}
