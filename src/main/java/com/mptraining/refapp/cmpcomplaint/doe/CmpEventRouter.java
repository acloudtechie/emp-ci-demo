/**
 *
 * Handles business logic for the CMP Event CTO for each
 *
 * administrator 10/04/2016
 **/

package com.mptraining.refapp.cmpcomplaint.doe;

import net.entellitrak.aea.auditLog.AeaAuditLog;
import net.entellitrak.aea.rf.RulesFramework;

import com.entellitrak.ApplicationException;
import com.entellitrak.DataEventType;
import com.entellitrak.DataObjectEventContext;
import com.entellitrak.tracking.DataObjectEventHandler;

public class CmpEventRouter implements DataObjectEventHandler {

	@Override
	public void execute(DataObjectEventContext etk) throws ApplicationException {

		AeaAuditLog auditLog = null;

		try {
			auditLog = new AeaAuditLog(etk);

			if (DataEventType.CREATE.equals(etk.getDataEventType())) {
				RulesFramework.updateWorkflow(etk, "workflow.cmpComplaint", etk.getNewObject().properties().getId());
			}

			if (!etk.getResult().isTransactionCanceled()) {
				auditLog.logETPChanges();
			}
		} catch (final Exception e) {
			// Handle any exceptions here
			throw new ApplicationException(e);
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
