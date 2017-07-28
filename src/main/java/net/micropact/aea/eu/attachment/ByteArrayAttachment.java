package net.micropact.aea.eu.attachment;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import net.entellitrak.aea.eu.IAttachment;

/**
 * Represents an email attachment as a byte[].
 * This of course means that the entire attachment is stored in memory.
 *
 * @author zmiller
 */
public class ByteArrayAttachment implements IAttachment {

    private final String name;
    private final byte[] content;

    /**
     * Construct a ByteArrayAttachment given an attachment name and byte[] content.
     *
     * @param attachmentName The name of the Email Attachment.
     * @param attachmentContent The binary content of the Email Attachment.
     */
    public ByteArrayAttachment(final String attachmentName, final byte[] attachmentContent){
        name = attachmentName;
        content = attachmentContent;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public InputStream getContent() {
        return new ByteArrayInputStream(content);
    }
}
