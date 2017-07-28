package net.entellitrak.aea.eu;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import com.entellitrak.ExecutionContext;

import net.entellitrak.aea.exception.EmailException;
import net.entellitrak.aea.exception.TemplateException;
import net.entellitrak.aea.tu.ITemplater;
import net.entellitrak.aea.tu.Templater;

/**
 * <p>
 *  This class is used to generate IEmails from templates existing in the T_EU_EMAIL_TEMPLATE reference data list.
 *  The Subject and Body of the Email will have an {@link ITemplater} run on them before being sent out.
 *  This class is not capable of actually sending emails.
 * </p>
 *
 * @author zmiller
 * @see EmailQueue
 * @see ITemplater
 * @see SimpleEmail
 * @see Templater
 */
public final class TemplateEmail{

    /**
     * Template emails will be generated from the generate methods, so the constructor is hidden.
     */
    private TemplateEmail(){}

    /**
     * This function will generate an email object and perform replacements on its Subject and Body.
     * This function does not actually send an Email.
     *
     * @param etk etk variable in entellitrak
     * @param emailCode The C_CODE column of the email you wish to send
     * @param recipients The addresses which should receive the email.
     *  Must be a valid <a href="doc-files/email-address.html">Email Utility Email Address</a>
     * @param ccRecipients The CC (Carbon Copy)
     *  Must be a valid <a href="doc-files/email-address.html">Email Utility Email Address</a>
     * @param bccRecipients The BCC (Blind Carbon Copy) addresses which should receive the email.
     *  Must be a valid <a href="doc-files/email-address.html">Email Utility Email Address</a>
     * @param attachments Attachments that should be sent with the email
     * @param replacementVariables These are variables that will be passed into the ITemplater
     * when it performs its replacement
     * @param templater The ITemplater to be run on the Subject and Body of the Email before being sent.
     * @return Resulting email with variables replaced
     * @throws EmailException If any problems are encountered
     *
     * @deprecated use {@link TemplateEmail#generate(ExecutionContext, String, javax.mail.internet.InternetAddress, Object, Object, Object, Collection, Map, ITemplater)}
     */
    @Deprecated
    public static IEmail generate(final ExecutionContext etk,
            final String emailCode,
            final Object recipients,
            final Object ccRecipients,
            final Object bccRecipients,
            final Collection<IAttachment> attachments,
            final Map <String, Object>replacementVariables,
            final ITemplater templater) throws EmailException{
        return generate(etk, emailCode, null, recipients, ccRecipients, bccRecipients, attachments, replacementVariables,
                templater);
    }

    /**
     * This function will generate an email object and perform replacements on its Subject and Body.
     * This function does not actually send an Email.
     *
     * @param etk etk variable in entellitrak
     * @param emailCode The C_CODE column of the email you wish to send
     * @param fromAddress From email address, or null to use the system default
     * @param recipients The addresses which should receive the email.
     *  Must be a valid <a href="doc-files/email-address.html">Email Utility Email Address</a>
     * @param ccRecipients The CC (Carbon Copy)
     *  Must be a valid <a href="doc-files/email-address.html">Email Utility Email Address</a>
     * @param bccRecipients The BCC (Blind Carbon Copy) addresses which should receive the email.
     *  Must be a valid <a href="doc-files/email-address.html">Email Utility Email Address</a>
     * @param attachments Attachments that should be sent with the email
     * @param replacementVariables These are variables that will be passed into the ITemplater
     * when it performs its replacement
     * @param templater The ITemplater to be run on the Subject and Body of the Email before being sent.
     * @return Resulting email with variables replaced
     * @throws EmailException If any problems are encountered
     */
    public static IEmail generate(final ExecutionContext etk,
            final String emailCode,
            final javax.mail.internet.InternetAddress fromAddress,
            final Object recipients,
            final Object ccRecipients,
            final Object bccRecipients,
            final Collection<IAttachment> attachments,
            final Map <String, Object>replacementVariables,
            final ITemplater templater) throws EmailException{

        final String errorString = String.format("Error generating email %s", emailCode);

        try {
            final List<Map<String, Object>> emailInfos = etk.createSQL("SELECT C_SUBJECT, C_BODY FROM t_eu_email_template WHERE c_code = :code")
                    .setParameter("code", emailCode)
                    .fetchList();
            if(emailInfos.size() == 0){
                throw new EmailException(String.format("Email not found: \"%s\"", emailCode));
            }else{
                final Map<String, Object> emailInfo = emailInfos.get(0); /*C_SUBJECT, C_BODY*/
                final String subject = templater.parse((String) emailInfo.get("C_SUBJECT"), replacementVariables);
                final String body = templater.parse((String) emailInfo.get("C_BODY"), replacementVariables);

                return new SimpleEmail(etk, subject, body, fromAddress, recipients, ccRecipients, bccRecipients,
                        attachments);
            }
        } catch (final TemplateException e) {
            throw new EmailException(errorString, e);
        }
    }

    /**
     *  This version of the function will use the default {@link Templater}
     *  It will not actually send an email.
     *
     * @param etk etk variable in entellitrak
     * @param emailCode The C_CODE column of the email you wish to send
     * @param recipients The addresses which should receive the email.
     *  Must be a valid <a href="doc-files/email-address.html">Email Utility Email Address</a>
     * @param ccRecipients The CC (Carbon Copy) addresses which should receive the email.
     *  Must be a valid <a href="doc-files/email-address.html">Email Utility Email Address</a>
     * @param bccRecipients The BCC (Blind Carbon Copy) addresses which should receive the email.
     *  Must be a valid <a href="doc-files/email-address.html">Email Utility Email Address</a>
     * @param attachments Attachments that should be sent with the email
     * @param replacementVariables These are variables that will be passed into the ITemplater
     *  when it performs its replacement
     * @return The resulting email with variables replaced
     * @throws EmailException If any problems are encountered
     *
     * @deprecated use {@link TemplateEmail#generate(ExecutionContext, String, InternetAddress, Object, Object, Object, Collection, Map)}
     */
    @Deprecated
    public static IEmail generate(final ExecutionContext etk,
            final String emailCode,
            final Object recipients,
            final Object ccRecipients,
            final Object bccRecipients,
            final Collection<IAttachment> attachments,
            final Map<String, Object> replacementVariables) throws EmailException{
        final ITemplater templater = new Templater(etk);
        return generate(etk,
                emailCode,
                null,
                recipients,
                ccRecipients,
                bccRecipients,
                attachments,
                replacementVariables,
                templater);
    }

    /**
     *  This version of the function will use the default {@link Templater}
     *  It will not actually send an email.
     *
     * @param etk etk variable in entellitrak
     * @param emailCode The C_CODE column of the email you wish to send
     * @param fromAddress From email address, or null to use the system default
     * @param recipients The addresses which should receive the email.
     *  Must be a valid <a href="doc-files/email-address.html">Email Utility Email Address</a>
     * @param ccRecipients The CC (Carbon Copy) addresses which should receive the email.
     *  Must be a valid <a href="doc-files/email-address.html">Email Utility Email Address</a>
     * @param bccRecipients The BCC (Blind Carbon Copy) addresses which should receive the email.
     *  Must be a valid <a href="doc-files/email-address.html">Email Utility Email Address</a>
     * @param attachments Attachments that should be sent with the email
     * @param replacementVariables These are variables that will be passed into the ITemplater
     *  when it performs its replacement
     * @return The resulting email with variables replaced
     * @throws EmailException If any problems are encountered
     */
    public static IEmail generate(final ExecutionContext etk,
            final String emailCode,
            final InternetAddress fromAddress,
            final Object recipients,
            final Object ccRecipients,
            final Object bccRecipients,
            final Collection<IAttachment> attachments,
            final Map<String, Object> replacementVariables) throws EmailException{
        final ITemplater templater = new Templater(etk);
        return generate(etk,
                emailCode,
                fromAddress,
                recipients,
                ccRecipients,
                bccRecipients,
                attachments,
                replacementVariables,
                templater);
    }
}
