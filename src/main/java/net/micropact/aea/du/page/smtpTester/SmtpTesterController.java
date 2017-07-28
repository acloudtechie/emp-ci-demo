package net.micropact.aea.du.page.smtpTester;

import java.util.Optional;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.legacy.util.StringUtility;
import com.entellitrak.mail.Mail;
import com.entellitrak.mail.MailException;
import com.entellitrak.mail.MailService;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.JsonUtilities;

/**
 * <p>
 *  This controller is for testing whether emails are sending correctly from the system.
 * </p>
 * <p>
 *  If entellitrak is not connected to the SMTP server then there will be an exception, or no email will be received.
 * </p>
 * @author zmiller
 */
public class SmtpTesterController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {
        final TextResponse response = etk.createTextResponse();

        String formAction = Optional.ofNullable(etk.getParameters().getSingle("formAction")).orElse("initial");
        final String recipient = etk.getParameters().getSingle("recipient");

        String error = null;
        String success = null;

        if("send".equals(formAction)){
            if (StringUtility.isBlank(recipient)){
                formAction = "error";
                error = "Recipient Address should not be blank";
            }else {
                final MailService mailService = etk.getMailService();
                final Mail mail = mailService.createMail();
                mail.setSubject("SMTP Tester");
                mail.setMessage("This is an SMTP test");
                mail.addTo(recipient);
                try {
                    mailService.send(mail);

                    success = "Test Email Sent";
                } catch (final MailException e) {
                    throw new ApplicationException(e);
                }
            }
        }

        response.put("success", JsonUtilities.encode(success));
        response.put("recipient", JsonUtilities.encode(recipient));
        response.put("error", JsonUtilities.encode(error));

        return response;
    }

}
