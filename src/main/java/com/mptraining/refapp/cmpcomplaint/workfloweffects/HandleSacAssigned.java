package com.mptraining.refapp.cmpcomplaint.workfloweffects;

import net.entellitrak.aea.exception.RulesFrameworkException;
import net.entellitrak.aea.rf.IRulesFrameworkParameters;
import net.entellitrak.aea.rf.IScript;

import com.entellitrak.DataEventType;
import com.entellitrak.DataObjectEventContext;
import com.entellitrak.ExecutionContext;
import com.entellitrak.InputValidationException;
import com.entellitrak.dynamic.CategoryType;
import com.entellitrak.dynamic.CmpComplaint;
import com.mptraining.refapp.common.script.AssignmentUtilities;

public class HandleSacAssigned implements IScript {

	@Override
	public void doEffect(ExecutionContext etk, IRulesFrameworkParameters parameters) throws RulesFrameworkException {

		DataObjectEventContext doec = (DataObjectEventContext) etk;
		CmpComplaint complaint = etk.getDynamicObjectService().get(CmpComplaint.class, doec.getNewObject().properties().getId());
		CategoryType categoryType = etk.getDynamicObjectService().get(CategoryType.class, complaint.getCategory().longValue());
		String categoryCode = categoryType.getCode();

		//Validate SAC Assigned is filled in when Category is Case
		if ((DataEventType.CREATE.equals(doec.getDataEventType()) || DataEventType.UPDATE.equals(doec.getDataEventType()))
				&& complaint.getSacAssigned() == null && "categoryType.case".equals(categoryCode)) {
			doec.getResult().addMessage("SAC Assigned is required when the Category chosen is Case");
			doec.getResult().cancelTransaction();
		}

		// Automatically assign user in SAC Assigned field to Complaint
		try {
			if (complaint.getSacAssigned() != null) {
				AssignmentUtilities.assignToUser(etk, complaint.properties().getId(), CmpComplaint.class, complaint.getSacAssigned().longValue(), "role.sac");
			}
		} catch (InputValidationException e) {
			e.printStackTrace();
			throw new RulesFrameworkException(e);
		}

	}

}
