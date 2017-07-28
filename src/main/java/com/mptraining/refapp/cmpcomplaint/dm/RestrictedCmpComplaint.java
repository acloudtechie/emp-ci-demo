/**
 *
 * Display mapping to evaluate if the Confidential and SAC Assigned fields should be Read Only
 *
 * administrator 10/06/2016
 **/

package com.mptraining.refapp.cmpcomplaint.dm;

import com.entellitrak.ApplicationException;
import com.entellitrak.dynamic.CmpComplaint;
import com.entellitrak.workflow.DisplayMappingExecutionContext;
import com.entellitrak.workflow.DisplayMappingHandler;

public class RestrictedCmpComplaint implements DisplayMappingHandler {

    @Override
	public Boolean execute(DisplayMappingExecutionContext etk) throws ApplicationException {

    	CmpComplaint complaint = (CmpComplaint) etk.getOpenObject();
    	String roleBK = etk.getCurrentUser().getRole().getBusinessKey();
        return !("role.administration".equals(roleBK) || "role.sac".equals(roleBK) || "role.specialAgent".equals(roleBK)) && (new Integer(1)).equals(complaint.getConfidential());
    }

}
