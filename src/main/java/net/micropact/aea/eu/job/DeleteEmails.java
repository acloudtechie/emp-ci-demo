package net.micropact.aea.eu.job;

import com.entellitrak.ApplicationException;
import com.entellitrak.scheduler.JobHandler;
import com.entellitrak.scheduler.SchedulerExecutionContext;

import net.micropact.aea.core.cache.AeaCoreConfiguration;
import net.micropact.aea.utility.Utility;

/**
 * Deletes emails from the queue which were created long ago (configured in the AEA Core Configuration RDO).
 *
 * @author zmiller
 */
public class DeleteEmails implements JobHandler {

    @Override
    public void execute(final SchedulerExecutionContext etk) throws ApplicationException {
        // This whole method could be in 1 SQL query but we should be able to provide better error messages this way.

        final Long days = AeaCoreConfiguration.getEuDaysUntilDeleteEmailsFromQueue(etk);

        // If it was blank, we don't delete anything.
        if(null != days){
            // Delete the emails.
            etk.createSQL(Utility.isSqlServer(etk) ? "DELETE FROM t_eu_email_queue WHERE CAST(c_created_time AS DATE) < CAST(DATEADD(DAY, 0 - :daysUntilDelete, DBO.ETKF_GETSERVERTIME()) AS DATE)"
                                                     : "DELETE FROM t_eu_email_queue WHERE TRUNC(c_created_time) < TRUNC(ETKF_GETSERVERTIME() - :daysUntilDelete)")
            .setParameter("daysUntilDelete", days)
            .execute();
        }

        /* Even if there we didn't delete any, delete attachments just in case the
         * user deleted something through the front end. */
        etk.createSQL("DELETE FROM t_eu_queue_attachment WHERE NOT EXISTS( SELECT * FROM t_eu_email_queue WHERE t_eu_email_queue.id = t_eu_queue_attachment.c_email_queue_id )")
        .execute();

        /* Delete any queue addresses which are not referenced anywhere */
        etk.createSQL("DELETE FROM t_eu_queue_address WHERE NOT EXISTS(SELECT * FROM t_eu_email_queue WHERE c_from_address = t_eu_queue_address.id)")
            .execute();
    }
}
