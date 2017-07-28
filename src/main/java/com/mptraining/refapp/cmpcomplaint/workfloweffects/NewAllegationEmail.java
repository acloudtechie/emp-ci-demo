package com.mptraining.refapp.cmpcomplaint.workfloweffects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.entellitrak.aea.eu.EmailQueue;
import net.entellitrak.aea.eu.IEmail;
import net.entellitrak.aea.eu.TemplateEmail;
import net.entellitrak.aea.exception.EmailException;
import net.entellitrak.aea.exception.RulesFrameworkException;
import net.entellitrak.aea.rf.IRulesFrameworkParameters;
import net.entellitrak.aea.rf.IScript;

import com.entellitrak.DataObjectEventContext;
import com.entellitrak.ExecutionContext;
import com.entellitrak.dynamic.CmpComplaint;
import com.mptraining.refapp.common.dao.EtkTablesDao;

public class NewAllegationEmail implements IScript {

	@Override
	public void doEffect(ExecutionContext etk, IRulesFrameworkParameters parameters) throws RulesFrameworkException {
		DataObjectEventContext doec = (DataObjectEventContext) etk;
		CmpComplaint complaint = etk.getDynamicObjectService().get(CmpComplaint.class, doec.getNewObject().properties().getId());
		if(!doec.getResult().isTransactionCanceled()){
			try {
				String generatedComplaintId = complaint.getComplaintId();

				//Send New Allegation Email
				List<String> recipients = EtkTablesDao.getGroupEmails(etk, "group.specialAgentTeam");
				if(!recipients.isEmpty()){
					Map<String, Object> replacementVariables = new HashMap<String, Object>();
					replacementVariables.put("CmpComplaintId", complaint.properties().getId());
					replacementVariables.put("GeneratedId", generatedComplaintId);
					replacementVariables.put("Current User Name", doec.getCurrentUser().getProfile().getFirstName() + " " + doec.getCurrentUser().getProfile().getLastName());
					IEmail email = TemplateEmail.generate(doec, "email.newCmpAllegation", recipients, null, null, null, replacementVariables);
					EmailQueue.queueEmail(doec, email);
				}
			} catch (EmailException e) {
				e.printStackTrace();
				throw new RulesFrameworkException(e);
			}
		}
	}
}
