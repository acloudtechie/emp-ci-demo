/**
 *
 * Display mapping to evaluate if the Confidential and SAC Assigned fields should be Read Only
 *
 * administrator 09/28/2016
 **/

package com.mptraining.refapp.complaint.dm;

import com.entellitrak.ApplicationException;
import com.entellitrak.dynamic.Complaint;
import com.entellitrak.workflow.DisplayMappingHandler;
import com.entellitrak.workflow.DisplayMappingExecutionContext;

public class RestrictedComplaint implements DisplayMappingHandler {

    @Override
	public Boolean execute(DisplayMappingExecutionContext etk) throws ApplicationException {
    	Complaint complaint = (Complaint) etk.getOpenObject();
    	String roleBK = etk.getCurrentUser().getRole().getBusinessKey();
        return !("role.administration".equals(roleBK) || "role.sac".equals(roleBK) || "role.specialAgent".equals(roleBK)) && (new Integer(1)).equals(complaint.getConfidential());
    }

}
