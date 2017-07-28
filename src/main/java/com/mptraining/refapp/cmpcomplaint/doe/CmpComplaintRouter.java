/**
 *
 * Handles business logic for CMP Complaint BTO for each CRUD action
 *
 * administrator 09/30/2016
 **/

package com.mptraining.refapp.cmpcomplaint.doe;

import net.entellitrak.aea.auditLog.AeaAuditLog;
import net.entellitrak.aea.rf.RulesFramework;

import com.entellitrak.ApplicationException;
import com.entellitrak.DataEventType;
import com.entellitrak.DataObjectEventContext;
import com.entellitrak.InputValidationException;
import com.entellitrak.WorkExecutionException;
import com.entellitrak.dynamic.CategoryType;
import com.entellitrak.dynamic.CmpComplaint;
import com.entellitrak.tracking.DataObjectEventHandler;
import com.mptraining.refapp.common.script.FormAuditFieldsUtility;

public class CmpComplaintRouter implements DataObjectEventHandler {

	@Override
	public void execute(DataObjectEventContext etk) throws ApplicationException {

		AeaAuditLog auditLog = null;

		try {
			auditLog = new AeaAuditLog(etk);

			CmpComplaint complaint = (CmpComplaint) etk.getNewObject();

			if(DataEventType.CREATE.equals(etk.getDataEventType()) || DataEventType.UPDATE.equals(etk.getDataEventType())){
				//Fill in audit fields
	     		FormAuditFieldsUtility.updateObjectAudit(etk, complaint);
			}

			if("true".equals(etk.getForm().getValue("deleted"))){
				complaint.setDeleted(true);
	      		try {
					etk.getDynamicObjectService().createSaveOperation(complaint).setExecuteEvents(false).save();
				} catch (InputValidationException e) {
					e.printStackTrace();
					throw new WorkExecutionException(e);
				}
				etk.getRedirectManager().redirectToUrl("tracking.dashBoard.do?dataObjectKey=object.cmpComplaint");
			}
			else if (DataEventType.CREATE.equals(etk.getDataEventType())) {
				// Initiate workflow
				RulesFramework.startWorkflow(etk, "workflow.cmpComplaint", etk.getNewObject().properties().getId());

				// Update workflow to transition to Open Case / Allegation as needed
				CategoryType categoryType = etk.getDynamicObjectService().get(CategoryType.class, complaint.getCategory().longValue());
				String categoryCode = categoryType.getCode();

				String transitionCode = "";
				if ("categoryType.allegation".equals(categoryCode)) {
					transitionCode = "transition.createAllegation";
				} else if ("categoryType.case".equals(categoryCode)) {
					transitionCode = "transition.createCase";
				}

				RulesFramework.insertChildWorkflow(etk, "workflow.cmpComplaint", etk.getNewObject().properties().getId(), transitionCode);
			}

			if (!etk.getResult().isTransactionCanceled()) {
				auditLog.logETPChanges();
			}
		} finally {
			// This must be called in a finally block. Your code must always call this, regardless of whether the
			// transaction rolled back.
			if (auditLog != null) {
				auditLog.writeAuditLogEntries();
				auditLog.closeConnection();
			}
		}

	}

}
