/**
 *
 * Scheduled Job to find past due events.
 *
 * administrator 10/06/2016
 **/

package com.mptraining.refapp.common.job;

import com.entellitrak.ApplicationException;
import com.entellitrak.scheduler.JobHandler;
import com.entellitrak.scheduler.SchedulerExecutionContext;
import com.mptraining.refapp.cmpcomplaint.dao.CmpComplaintDao;
import com.mptraining.refapp.complaint.dao.ComplaintDao;

public class FindPastDueEvents implements JobHandler {

    @Override
	public void execute(SchedulerExecutionContext etk) throws ApplicationException {

    	ComplaintDao.clearMessages(etk);
    	ComplaintDao.setPastDueEventMessages(etk);

    	CmpComplaintDao.clearMessages(etk);
    	CmpComplaintDao.setPastDueEventMessages(etk);

    }

}
