/**
 *
 * Handles business logic for Complaint BTO for each CRUD operation.  If logic is extensive, this will route logic to other classes based on the operation performed.
 *
 * administrator 09/27/2016
 **/

package com.mptraining.refapp.complaint.doe;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.entellitrak.aea.eu.EmailQueue;
import net.entellitrak.aea.eu.IEmail;
import net.entellitrak.aea.eu.TemplateEmail;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import com.entellitrak.ApplicationException;
import com.entellitrak.DataEventType;
import com.entellitrak.DataObjectEventContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.InputValidationException;
import com.entellitrak.dynamic.CategoryType;
import com.entellitrak.dynamic.Complaint;
import com.entellitrak.dynamic.Event;
import com.entellitrak.dynamic.SystemInformation;
import com.entellitrak.tracking.DataObjectEventHandler;
import com.mptraining.refapp.common.dao.EtkTablesDao;
import com.mptraining.refapp.common.dao.SequenceDao;
import com.mptraining.refapp.common.script.AssignmentUtilities;
import com.mptraining.refapp.common.script.FormAuditFieldsUtility;
import com.mptraining.refapp.rdo.dao.RdoDao;

public class ComplaintRouter implements DataObjectEventHandler {

	@Override
	public void execute(DataObjectEventContext etk) throws ApplicationException {

        Complaint complaint = (Complaint) etk.getNewObject();
        CategoryType categoryType = etk.getDynamicObjectService().get(CategoryType.class, complaint.getCategory().longValue());
		String categoryCode = categoryType.getCode();

 		//Validate SAC Assigned is filled in when Category is Case
     	if((DataEventType.CREATE.equals(etk.getDataEventType()) || DataEventType.UPDATE.equals(etk.getDataEventType()))
     			&& complaint.getSacAssigned() == null && "categoryType.case".equals(categoryCode)){
     			etk.getResult().addMessage("SAC Assigned is required when the Category chosen is Case");
         		etk.getResult().cancelTransaction();
     	}
     	else{
     		//Fill in audit fields
         	FormAuditFieldsUtility.updateObjectAudit(etk, complaint);
     	}

     	//Make sure Source is set to efiler for efiler users
     	if(DataEventType.CREATE.equals(etk.getDataEventType()) && "role.efiler".equals(etk.getCurrentUser().getRole().getBusinessKey())
     			&& complaint.getSource() == null){
     			try {
     				complaint.setSource(RdoDao.getIdFromCode(etk, "T_SOURCE_TYPE", "sourceType.efiler"));
					etk.getDynamicObjectService().createSaveOperation(complaint).setExecuteEvents(false).save();
				} catch (InputValidationException | IncorrectResultSizeDataAccessException e) {
					e.printStackTrace();
					throw new ApplicationException(e);
				}
     	}

     	if(DataEventType.CREATE.equals(etk.getDataEventType()) && !etk.getResult().isTransactionCanceled()){
     		String eventType = null;
     		String message = null;
     		boolean sendAllegationEmail = false;

 			if("categoryType.allegation".equals(categoryCode)){
 				eventType = "eventType.createAllegation";
 				sendAllegationEmail = true;
 				message = "Allegation Created";
 			}
 			else if("categoryType.case".equals(categoryCode)){
 				eventType = "eventType.createCase";
 				message = "Case Created";
 			}
 			etk.getResult().addMessage(message);

 			//If there is a set SAC, assign the Complaint to them
     		if(complaint.getSacAssigned() != null){
				try {
					AssignmentUtilities.assignToUser(etk, complaint.properties().getId(), Complaint.class, complaint.getSacAssigned().longValue(), "role.sac");
				} catch (InputValidationException e) {
					e.printStackTrace();
					throw new ApplicationException(e);
				}
			}

     		//Assign any complaints created by an efiler to the Special Agent Team Group
     		if("role.efiler".equals(etk.getCurrentUser().getRole().getBusinessKey())){
     			try {
					AssignmentUtilities.assignToGroup(etk, complaint.properties().getId(), Complaint.class, "group.specialAgentTeam", "role.specialAgent");
				} catch (InputValidationException e) {
					e.printStackTrace();
					throw new ApplicationException(e);
				}
     		}

     		//Autogenerate Complaint ID
     		String generatedComplaintId = null;
     		SystemInformation yearInfo = null;
     		Integer currentYear = new DateTime().getYear();

     		//Get year info from System Information RDO
			try {
				yearInfo = etk.getDynamicObjectService().get(SystemInformation.class, RdoDao.getIdFromCode(etk, "T_SYSTEM_INFORMATION", "complaintIdSeqYear").longValue());
			} catch (IncorrectResultSizeDataAccessException e) {
				//If there is no existing record, create one
				yearInfo = etk.getDynamicObjectService().createNew(SystemInformation.class);
				yearInfo.setName("Complaint ID Sequence Year");
				yearInfo.setCode("complaintIdSeqYear");
				yearInfo.setValue(currentYear.toString());
				try {
					etk.getDynamicObjectService().createSaveOperation(yearInfo).save();
				} catch (InputValidationException e1) {
					e1.printStackTrace();
					throw new ApplicationException(e1);
				}
			}
     		try {
 				//If the year from System Information differs from the current year, regenerate the sequence so it starts at 0 again
 				if(!yearInfo.getValue().equals(currentYear.toString())){
 					SequenceDao.dropComplaintIdSeq(etk);
					SequenceDao.createComplaintIdSeq(etk);
					yearInfo.setValue(currentYear.toString());
					etk.getDynamicObjectService().createSaveOperation(yearInfo).save();
 				}

 				Integer seqNum = -1;
 				try{
 					seqNum = SequenceDao.selectFromComplaintIdSeq(etk);
 				}catch(Exception e){
 					SequenceDao.createComplaintIdSeq(etk);
 					seqNum = SequenceDao.selectFromComplaintIdSeq(etk);
 				}
 				
 				generatedComplaintId = currentYear + "-" + StringUtils.leftPad(seqNum.toString(), 4, '0');

 				complaint.setComplaintId(generatedComplaintId);
 				etk.getDynamicObjectService().createSaveOperation(complaint).setExecuteEvents(false).save();

 			} catch (InputValidationException e) {
 				e.printStackTrace();
 				throw new ApplicationException(e);
 			}

     		//Send New Allegation Email
     		if(sendAllegationEmail){
     			List<String> recipients = EtkTablesDao.getGroupEmails(etk, "group.specialAgentTeam");
     			if(!recipients.isEmpty()){
     				Map<String, Object> replacementVariables = new HashMap<String, Object>();
     				replacementVariables.put("ComplaintId", complaint.properties().getId());
     				replacementVariables.put("GeneratedId", generatedComplaintId);
     				replacementVariables.put("Current User Name", etk.getCurrentUser().getProfile().getFirstName() + " " + etk.getCurrentUser().getProfile().getLastName());
     				IEmail email = TemplateEmail.generate(etk, "email.newAllegation", recipients, null, null, null, replacementVariables);
     				EmailQueue.queueEmail(etk, email);
     			}
     		}

     		//Create a new event indicating Case / Allegation creation
 			try {
 				Event event = etk.getDynamicObjectService().createNew(Event.class);
 	 			event.setEventType(RdoDao.getIdFromCode(etk, "T_EVENT_TYPE", eventType));
 	 			event.setStartDate(new Date());
				etk.getDynamicObjectService().createSaveOperation(event).setExecuteEvents(true).setParent(complaint).save();
			} catch (InputValidationException | IncorrectResultSizeDataAccessException e) {
				e.printStackTrace();
				throw new ApplicationException(e);
			}
     	}
    }
}
