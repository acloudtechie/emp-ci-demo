package net.micropact.aea.eu.job;

import com.entellitrak.ApplicationException;
import com.entellitrak.scheduler.JobHandler;
import com.entellitrak.scheduler.SchedulerExecutionContext;

import net.micropact.aea.eu.utility.EmailQueueSender;

/**
 * This class is responsible for sending emails in the email queue.
 * It will send all emails which have not already been sent and log errors if they occur.
 * If an email cannot be sent, the error will be logged and it will attempt te resend the emails later.
 *
 * @author zmiller
 * @see net.entellitrak.aea.eu.EmailQueue
 */
public class SendEmails implements JobHandler {

    @Override
    public void execute(final SchedulerExecutionContext etk)
            throws ApplicationException {
        EmailQueueSender.sendQueuedEmails(etk, false);
    }
}
