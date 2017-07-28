/**
 *
 * Assign any complaints created by an efiler to the Special Agent Team Group
 *
 * administrator 10/26/2016
 **/

package com.mptraining.refapp.cmpcomplaint.workfloweffects;

import net.entellitrak.aea.exception.RulesFrameworkException;
import net.entellitrak.aea.rf.IRulesFrameworkParameters;
import net.entellitrak.aea.rf.IScript;

import com.entellitrak.DataObjectEventContext;
import com.entellitrak.ExecutionContext;
import com.entellitrak.InputValidationException;
import com.entellitrak.dynamic.CmpComplaint;
import com.mptraining.refapp.common.script.AssignmentUtilities;

public class EfiledComplaintAssignment implements IScript{

	@Override
	public void doEffect(ExecutionContext etk, IRulesFrameworkParameters parameters) throws RulesFrameworkException {

		if("role.efiler".equals(etk.getCurrentUser().getRole().getBusinessKey())){
			DataObjectEventContext doec = (DataObjectEventContext) etk;
			CmpComplaint complaint = etk.getDynamicObjectService().get(CmpComplaint.class, doec.getNewObject().properties().getId());
 			try {
				AssignmentUtilities.assignToGroup(etk, complaint.properties().getId(), CmpComplaint.class, "group.specialAgentTeam", "role.specialAgent");
			} catch (InputValidationException e) {
				e.printStackTrace();
				throw new RulesFrameworkException(e);
			}
 		}

	}


}
