/**
 *
 * Display mapping to evaluate if a complaint is closed
 *
 * administrator 10/06/2016
 **/

package com.mptraining.refapp.cmpcomplaint.dm;

import com.entellitrak.ApplicationException;
import com.entellitrak.dynamic.CmpComplaint;
import com.entellitrak.dynamic.RfState;
import com.entellitrak.workflow.DisplayMappingExecutionContext;
import com.entellitrak.workflow.DisplayMappingHandler;

public class ClosedCmpComplaint implements DisplayMappingHandler {

    @Override
	public Boolean execute(DisplayMappingExecutionContext etk) throws ApplicationException {
    	CmpComplaint complaint = (CmpComplaint) etk.getOpenObject();

    	//Uses DynamicObjectService (instead of SQL) to get the current state and evaluates if it's closed
        if(complaint.getCurrentStatus() != null){
        	RfState state = etk.getDynamicObjectService().get(RfState.class, complaint.getCurrentStatus().longValue());
	        return "state.closed".equals(state.getCode());
        }

        return false;
    }

}
