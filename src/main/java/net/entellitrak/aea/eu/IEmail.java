package net.entellitrak.aea.eu;

import java.util.Collection;

import javax.mail.internet.InternetAddress;

/**
 * This class represents the information that is necessary to send an email.
 *
 * @author zmiller
 * @see EmailQueue
 */
public interface IEmail {

    /**
     * Get the subject line of the email.
     *
     * @return The Subject line of the email
     */
    String getSubject();

    /**
     * Get the body content of the email.
     *
     * @return The Body text of the email
     */
    String getBody();

    /**
     * Get the recipient addresses of the email.
     *
     * @return The To Addresses that the email was sent to
     */
    Collection<InternetAddress> getRecipients();

    /**
     * Get the CC recipients of the email.
     *
     * @return The CC (Carbon Copy) recipients of the email
     */
    Collection<InternetAddress> getCcRecipients();

    /**
     * Get the BCC recipients of the email.
     *
     * @return The BCC (Blind Carbon Copy) recipients of the email
     */
    Collection<InternetAddress> getBccRecipients();

    /**
     * Get the file attachments of the email.
     *
     * @return The files attached to the email
     */
    Collection<IAttachment> getAttachments();

    /**
     * Get the from address for the email.
     * Defaults to the system default for backwards compatibility.
     *
     * @return the from address, or null if it should use the system default.
     */
    default InternetAddress getFrom() {
        return null;
    }
}
