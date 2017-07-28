/**
 *
 * Display mapping to evaluate if a complaint is closed
 *
 * administrator 09/28/2016
 **/

package com.mptraining.refapp.complaint.dm;

import com.entellitrak.ApplicationException;
import com.entellitrak.dynamic.Complaint;
import com.entellitrak.dynamic.Status;
import com.entellitrak.workflow.DisplayMappingExecutionContext;
import com.entellitrak.workflow.DisplayMappingHandler;

public class ClosedComplaint implements DisplayMappingHandler {

    @Override
	public Boolean execute(DisplayMappingExecutionContext etk) throws ApplicationException {
        Complaint complaint = (Complaint) etk.getOpenObject();

      //Uses DynamicObjectService (instead of SQL) to get the current status and evaluates if it's closed
        if(complaint.getCurrentStatus() != null){
			Status status = etk.getDynamicObjectService().get(Status.class, complaint.getCurrentStatus().longValue());
	        return "status.closed".equals(status.getCode());
        }

        return false;

    }

}
