package net.micropact.aea.eu.attachment;

import java.io.InputStream;

import com.entellitrak.ExecutionContext;
import com.entellitrak.dm.DocumentManagementOperationException;

import net.entellitrak.aea.eu.IAttachment;
import net.entellitrak.aea.exception.EmailException;

/**
 * This represents an Email Attachment which has its contents stored in Document Management.
 *
 * @author zmiller
 */
public class DocumentManagementAttachment implements IAttachment {

    private final String name;
    private final ExecutionContext etk;
    private final long fileId;

    /**
     * Generate a new attachment from a file stored in Document Management.
     * Note: The attachmentName is being passed in even though it is redundant for reasons of efficiency.
     * The calling code can get the name when it is figuring out whether the document is stored in document management
     * or etk_file.
     *
     * @param executionContext Entellitrak execution context.
     * @param attachmentName Name of the attachment
     * @param etkFileId etk_file.id of the attachment within entellitrak.
     */
    public DocumentManagementAttachment(final ExecutionContext executionContext,
            final String attachmentName,
            final long etkFileId){
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
            return etk.getDocumentManagementServiceFactory().getFileService().getContentStream(fileId);
        } catch (final DocumentManagementOperationException e) {
            throw new EmailException(
                    String.format("Exception encountered getting email attachment file contents from Document Management for file: %s.",
                            fileId),
                            e);
        }
    }
}
