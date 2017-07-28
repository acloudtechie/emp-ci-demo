package net.micropact.aea.eu.attachment;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.entellitrak.ExecutionContext;

import net.entellitrak.aea.eu.IAttachment;
import net.entellitrak.aea.exception.EmailException;

/**
 * This class represents an Email Attachment whose content is stored in etk_file.
 *
 * @author zmiller
 */
public class EtkFileAttachment implements IAttachment {

    private final long fileId;
    private final String name;
    private final ExecutionContext etk;

    /**
     * Creates an attachment. Note: Currently the attachmentName is being passed in even though it is redundant
     * for reasons of efficiency. (The calling code can find out the name when it finds out whether the file in
     * etk_file or document management.)
     *
     * @param executionContext Entellitrak execution context.
     * @param attachmentName Name of the attachment.
     * @param etkFileId etk_file.id of the attachment content.
     */
    public EtkFileAttachment(final ExecutionContext executionContext, final String attachmentName, final long etkFileId){
        etk = executionContext;
        name = attachmentName;
        fileId = etkFileId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public InputStream getContent() throws EmailException {
        try {
            return new ByteArrayInputStream((byte[]) etk.createSQL("SELECT CONTENT FROM etk_file WHERE id = :fileId")
                    .setParameter("fileId", fileId)
                    .fetchObject());
        } catch (final Exception e) {
            throw new EmailException(String.format("Error attempting to get Email Attachment Content for file stored in etk_file for etk_file.id: %s.",
                    fileId), e);
        }
    }
}
