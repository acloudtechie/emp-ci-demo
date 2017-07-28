package net.entellitrak.aea.eu;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.LinkedList;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.entellitrak.ExecutionContext;

import net.entellitrak.aea.exception.EmailException;
import net.micropact.aea.eu.utility.InternetAddressUtility;

/**
 * <p>
 *  This class is used for generating simple emails that have nothing special such as replacements run on them.
 *  This class does not actually send any emails, but just represents the data for an email.
 * </p>
 *
 * @author zmiller
 * @see EmailQueue
 * @see TemplateEmail
 */
public final class SimpleEmail implements IEmail{

    private final String subject;
    private final String body;
    private final Collection<InternetAddress> recipients;
    private final Collection<InternetAddress> ccRecipients;
    private final Collection<InternetAddress> bccRecipients;
    private final Collection<IAttachment> attachments;
    private final InternetAddress from;

    /**
     * Constructs a SimpleEmail object.
     *
     * @param etk etk variable in entellitrak
     * @param emailSubject Subject of the Email
     * @param emailBody Body text of the Email
     * @param emailRecipients The addresses which should receive the email.
     *     Must be a valid <a href="doc-files/email-address.html">Email Utility Email Address</a>
     * @param emailCCRecipients The CC (Carbon Copy) addresses which should receive the email.
     *     Must be a valid <a href="doc-files/email-address.html">Email Utility Email Address</a>
     * @param emailBCCRecipients The BCC (Blind Carbon Copy) addresses which should receive the email.
     *     Must be a valid <a href="doc-files/email-address.html">Email Utility Email Address</a>
     * @param emailAttachments Attachments that should be sent with the email
     * @throws EmailException If any problem occurs
     *
     * @deprecated use {@link SimpleEmail#SimpleEmail(ExecutionContext, String, String, InternetAddress, Object, Object, Object, Collection)}
     */
    @Deprecated
    public SimpleEmail(final ExecutionContext etk,
            final String emailSubject,
            final String emailBody,
            final Object emailRecipients,
            final Object emailCCRecipients,
            final Object emailBCCRecipients,
            final Collection<IAttachment> emailAttachments) throws EmailException{
        this(etk, emailSubject, emailBody, null, emailRecipients, emailCCRecipients, emailBCCRecipients,
                emailAttachments);
    }

    /**
     * Constructs a SimpleEmail object.
     *
     * @param etk etk variable in entellitrak
     * @param emailSubject Subject of the Email
     * @param emailBody Body text of the Email
     * @param fromAddress From email address, or null to use the system default
     * @param emailRecipients The addresses which should receive the email.
     *     Must be a valid <a href="doc-files/email-address.html">Email Utility Email Address</a>
     * @param emailCCRecipients The CC (Carbon Copy) addresses which should receive the email.
     *     Must be a valid <a href="doc-files/email-address.html">Email Utility Email Address</a>
     * @param emailBCCRecipients The BCC (Blind Carbon Copy) addresses which should receive the email.
     *     Must be a valid <a href="doc-files/email-address.html">Email Utility Email Address</a>
     * @param emailAttachments Attachments that should be sent with the email
     * @throws EmailException If any problem occurs
     */
    public SimpleEmail(@SuppressWarnings("unused") final ExecutionContext etk,
            final String emailSubject,
            final String emailBody,
            final InternetAddress fromAddress,
            final Object emailRecipients,
            final Object emailCCRecipients,
            final Object emailBCCRecipients,
            final Collection<IAttachment> emailAttachments) throws EmailException{
        try {
            subject = emailSubject;
            body = emailBody;
            from = fromAddress;
            recipients = InternetAddressUtility.toInternetAddresses(emailRecipients);
            ccRecipients = InternetAddressUtility.toInternetAddresses(emailCCRecipients);
            bccRecipients = InternetAddressUtility.toInternetAddresses(emailBCCRecipients);

            if(emailAttachments == null){
                attachments = new LinkedList<>();
            }else{
                attachments = emailAttachments;
            }
        } catch (final AddressException e) {
            throw new EmailException(String.format("Error encountered in email addresses"), e);
        } catch (final UnsupportedEncodingException e) {
            throw new EmailException(String.format("Error encountered in email addresses"), e);
        }
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public Collection<InternetAddress> getRecipients() {
        return recipients;
    }

    @Override
    public Collection<InternetAddress> getCcRecipients() {
        return ccRecipients;
    }

    @Override
    public Collection<InternetAddress> getBccRecipients() {
        return bccRecipients;
    }

    @Override
    public Collection<IAttachment> getAttachments() {
        return attachments;
    }

    @Override
    public InternetAddress getFrom() {
        return from;
    }
}
