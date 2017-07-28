package net.micropact.aea.eu.doe;

import com.entellitrak.DataEventType;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.ReferenceObjectEventContext;
import com.entellitrak.dynamic.EuQueueAttachment;

import net.micropact.aea.core.doe.AReferenceObjectEventHandler;
import net.micropact.aea.utility.Utility;

/**
 * This is the Data Object Event Handler for the EU Queue Attachment object.
 *
 * @author zachary.miller
 */
public class EuQueueAttachmentDoe extends AReferenceObjectEventHandler {

    @Override
    protected void executeObject(final ReferenceObjectEventContext etk) throws IncorrectResultSizeDataAccessException {
        validateEmailQueueId(etk);
    }

    /**
     * This method ensures that the email queue id field on this attachment object points to a valid email queue.
     * If it does not, the user is provided with a message and the transaction is cancelled.
     *
     * @param etk entellitrak execution context
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    public static void validateEmailQueueId(final ReferenceObjectEventContext etk)
            throws IncorrectResultSizeDataAccessException{
        final DataEventType eventType = etk.getDataEventType();

        if(DataEventType.CREATE == eventType
                || DataEventType.UPDATE == eventType){

            final EuQueueAttachment queueAttachment = (EuQueueAttachment) etk.getNewObject();
            final long emailQueueId = queueAttachment.getEmailQueueId();

            if(0 == etk.createSQL("SELECT COUNT(*) FROM t_eu_email_queue WHERE id = :queueId")
                    .setParameter("queueId", emailQueueId)
                    .fetchInt()){
                Utility.cancelTransactionMessage(etk,
                        String.format("There is no EU Email Queue entry with a tracking id of \"%s\"",
                                emailQueueId));
            }
        }
    }
}
