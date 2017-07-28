package net.entellitrak.aea.eu;

import java.io.InputStream;

import net.entellitrak.aea.exception.EmailException;

/**
 * <p>This represents a file which can be attached to an email.</p>
 *
 * @author zmiller
 * @see IEmail
 */
public interface IAttachment {

    /**
     * Get the file name of the attachment.
     *
     * @return The name of the attached file
     */
    String getName();

    /**
     * Gets the contents of the File.
     * <strong>
     *  Returns a new {@link InputStream} every time it is called.
     *  If you call this method, you are responsible for making sure that the {@link InputStream} gets closed.
     * </strong>
     *
     * @return The content of the file
     * @throws EmailException If there was any problem getting the content of the attachment.
     */
    InputStream getContent() throws EmailException;
}
