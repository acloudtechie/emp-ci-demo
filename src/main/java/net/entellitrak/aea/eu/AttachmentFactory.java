package net.entellitrak.aea.eu;

import java.util.Map;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;

import net.entellitrak.aea.exception.EmailException;
import net.micropact.aea.eu.attachment.ByteArrayAttachment;
import net.micropact.aea.eu.attachment.DocumentManagementAttachment;
import net.micropact.aea.eu.attachment.EtkFileAttachment;
import net.micropact.aea.utility.FileType;

/**
 * Factory class for generating {@link IAttachment}s.
 *
 * @author zmiller
 */
public final class AttachmentFactory{

    /**
     * There is no reason to create a new {@link AttachmentFactory}.
     */
    private AttachmentFactory(){}

    /**
     * Generates an attachment with the given name and attachment content.
     *
     * @param attachmentName Name of the Attachment.
     * @param attachmentContent Content of the Attachment.
     * @return An {@link IAttachment}.
     */
    public static IAttachment generateAttachment(final String attachmentName, final byte[] attachmentContent){
        return new ByteArrayAttachment(attachmentName, attachmentContent);
    }

    /**
     * Generates an attachment from a file that exists in ETK_FILE.
     * This includes files that are part of entellidoc since they still have an ETK_FILE entry.
     *
     * @param etk etk variable in entellitrak
     * @param fileId ID of the file in ETK_FILE
     * @return An {@link IAttachment}.
     * @throws EmailException If any problem occurs.
     */
    public static IAttachment generateAttachment(final ExecutionContext etk, final long fileId) throws EmailException{
        try {
            final Map<String, Object> fileInfo = etk.createSQL("SELECT FILE_NAME, FILE_TYPE FROM etk_file WHERE id = :fileId")
                    .setParameter("fileId", fileId)
                    .fetchMap();

            final String name = (String) fileInfo.get("FILE_NAME");
            if(FileType.DOCUMENT_MANAGEMENT_FILE.getEntellitrakNumber()
                    == ((Number) (fileInfo.get("FILE_TYPE"))).intValue()){
                return new DocumentManagementAttachment(etk, name, fileId);
            }else{
                return new EtkFileAttachment(etk, name, fileId);
            }
        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new EmailException(String.format("Exception generating attachment for fileId: %s", fileId), e);
        }
    }
}
