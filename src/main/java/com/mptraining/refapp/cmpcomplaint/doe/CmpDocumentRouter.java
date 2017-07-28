/**
 *
 * Handles business logic for CMP Document for each CRUD operation
 *
 * administrator 10/25/2016
 **/

package com.mptraining.refapp.cmpcomplaint.doe;

import net.entellitrak.aea.auditLog.AeaAuditLog;

import com.entellitrak.ApplicationException;
import com.entellitrak.DataObjectEventContext;
import com.entellitrak.tracking.DataObjectEventHandler;

public class CmpDocumentRouter implements DataObjectEventHandler {

	@Override
	public void execute(DataObjectEventContext etk) throws ApplicationException {

		AeaAuditLog auditLog = null;

		try {
			auditLog = new AeaAuditLog(etk);

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
