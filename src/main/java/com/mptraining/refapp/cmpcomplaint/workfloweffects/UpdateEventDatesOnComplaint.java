package com.mptraining.refapp.cmpcomplaint.workfloweffects;

import net.entellitrak.aea.exception.RulesFrameworkException;
import net.entellitrak.aea.rf.IRulesFrameworkParameters;
import net.entellitrak.aea.rf.IScript;

import com.entellitrak.DataObjectEventContext;
import com.entellitrak.ExecutionContext;
import com.entellitrak.InputValidationException;
import com.entellitrak.dynamic.CmpComplaint;
import com.entellitrak.dynamic.CmpEvent;

public class UpdateEventDatesOnComplaint implements IScript {

	@Override
	public void doEffect(ExecutionContext etk, IRulesFrameworkParameters parameters) throws RulesFrameworkException {
		DataObjectEventContext doec = (DataObjectEventContext) etk;
		Long id = null;
		if(doec.getNewObject() instanceof CmpComplaint){
			id = doec.getNewObject().properties().getId();
		}
		else{
			id = doec.getNewObject().properties().getBaseId();
		}
		CmpComplaint complaint = etk.getDynamicObjectService().get(CmpComplaint.class, id);
		CmpEvent event = etk.getDynamicObjectService().get(CmpEvent.class, parameters.getDefaultParameters().getChildTrackingId());

		complaint.setLastEventDate(event.getStartDate());
		complaint.setDueDate(event.getDueDate());
		try {
			etk.getDynamicObjectService().createSaveOperation(complaint).setExecuteEvents(false).save();
		} catch (InputValidationException e) {
			e.printStackTrace();
			throw new RulesFrameworkException(e);
		}

	}

}
