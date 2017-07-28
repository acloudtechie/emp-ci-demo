/**
 *
 * Email SAC Assigned 3 days before Event Due Date for Open Complaints
 *
 * administrator 10/07/2016
 **/

package com.mptraining.refapp.common.job;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.entellitrak.aea.eu.EmailQueue;
import net.entellitrak.aea.eu.IEmail;
import net.entellitrak.aea.eu.TemplateEmail;

import com.entellitrak.ApplicationException;
import com.entellitrak.scheduler.JobHandler;
import com.entellitrak.scheduler.SchedulerExecutionContext;
import com.mptraining.refapp.cmpcomplaint.dao.CmpComplaintDao;
import com.mptraining.refapp.complaint.dao.ComplaintDao;

public class NotifyAlmostDueEvents implements JobHandler {

    @Override
	public void execute(SchedulerExecutionContext etk) throws ApplicationException {

    	//Complaint
    	List<Map<String,Object>> approachingDueEvents = ComplaintDao.getApproachingDueEventInfo(etk);

    	for (Map<String, Object> event : approachingDueEvents) {
    		Map<String, Object> replacementVariables = new HashMap<String, Object>();
    		replacementVariables.put("ComplaintId", event.get("COMP_ID"));
    		replacementVariables.put("EventId", event.get("EVENT_ID"));
    		replacementVariables.put("eventType", event.get("EVENT_TYPE"));
    		IEmail email = TemplateEmail.generate(etk, "email.eventDue", event.get("EMAIL_ADDRESS"), null, null, null, replacementVariables);
    		EmailQueue.queueEmail(etk, email);
		}


    	//CMP Complaint
    	List<Map<String,Object>> cmpApproachingDueEvents = CmpComplaintDao.getApproachingDueEventInfo(etk);

    	for (Map<String, Object> event : cmpApproachingDueEvents) {
    		Map<String, Object> replacementVariables = new HashMap<String, Object>();
    		replacementVariables.put("CmpComplaintId", event.get("COMP_ID"));
    		replacementVariables.put("CmpEventId", event.get("EVENT_ID"));
    		replacementVariables.put("cmpEventType", event.get("EVENT_TYPE"));
    		IEmail email = TemplateEmail.generate(etk, "email.cmpEventDue", event.get("EMAIL_ADDRESS"), null, null, null, replacementVariables);
    		EmailQueue.queueEmail(etk, email);
		}
    }

}
