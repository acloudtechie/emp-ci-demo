/**
 *
 * Handles business logic for Event CTO for each CRUD operation.  If logic is extensive, this will route logic to other classes based on the operation performed.
 *
 * administrator 09/27/2016
 **/

package com.mptraining.refapp.complaint.doe;

import java.util.ArrayList;
import java.util.List;

import com.entellitrak.ApplicationException;
import com.entellitrak.DataEventType;
import com.entellitrak.DataObjectEventContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.InputValidationException;
import com.entellitrak.dynamic.Complaint;
import com.entellitrak.dynamic.Event;
import com.entellitrak.dynamic.EventType;
import com.entellitrak.dynamic.Status;
import com.entellitrak.tracking.DataObjectEventHandler;
import com.mptraining.refapp.rdo.dao.RdoDao;

public class EventRouter implements DataObjectEventHandler {

    @Override
	public void execute(DataObjectEventContext etk) throws ApplicationException {

    	if(DataEventType.CREATE.equals(etk.getDataEventType())){
    		Event event = (Event) etk.getNewObject();

    		Complaint complaint = etk.getDynamicObjectService().get(Complaint.class, event.properties().getParentId());

    		String currentStatusCode = null;

    		if(complaint.getCurrentStatus() != null){
    			Status currentStatus = etk.getDynamicObjectService().get(Status.class, complaint.getCurrentStatus().longValue());
    			currentStatusCode = currentStatus.getCode();
    		}

    		EventType eventType = etk.getDynamicObjectService().get(EventType.class, event.getEventType().longValue());
			String eventTypeCode = eventType.getCode();

			List<String> validNewComplaintEvents = new ArrayList<String>();
			validNewComplaintEvents.add("eventType.createAllegation");
			validNewComplaintEvents.add("eventType.createCase");

			List<String> validOpenAllegationEvents = new ArrayList<String>();
			validOpenAllegationEvents.add("eventType.accepted");
			validOpenAllegationEvents.add("eventType.cancel");
			validOpenAllegationEvents.add("eventType.conductPolygraph");
			validOpenAllegationEvents.add("eventType.newHotlineRequest");
			validOpenAllegationEvents.add("eventType.promoteAllegation");
			validOpenAllegationEvents.add("eventType.reviewComplaint");

			List<String> validOpenCaseEvents = new ArrayList<String>();
			validOpenCaseEvents.add("eventType.cancel");
			validOpenCaseEvents.add("eventType.closeCase");
			validOpenCaseEvents.add("eventType.conductPolygraph");
			validOpenCaseEvents.add("eventType.newHotlineRequest");
			validOpenCaseEvents.add("eventType.reviewComplaint");
			validOpenCaseEvents.add("eventType.searchAndSeizure");

			List<String> validClosedEvents = new ArrayList<String>();
			validClosedEvents.add("eventType.reviewComplaint");

    		boolean valid = ("status.openAllegation".equals(currentStatusCode) && validOpenAllegationEvents.contains(eventTypeCode))
    				|| ("status.openCase".equals(currentStatusCode) && validOpenCaseEvents.contains(eventTypeCode))
    				|| ("status.closed".equals(currentStatusCode) && validClosedEvents.contains(eventTypeCode))
    				|| (currentStatusCode == null && validNewComplaintEvents.contains(eventTypeCode));

    		if(!valid){
    			etk.getResult().addMessage("This event cannot be created from the current Complaint status.");
    			etk.getResult().cancelTransaction();
    		} else{
	    		String status = null;
	    		String message = null;

				if("eventType.closeCase".equals(eventTypeCode)){
					status = "status.closed";
					message = "Case Closed";
				}
				else if("eventType.cancel".equals(eventTypeCode)){
					status = "status.closed";
					message = "Complaint Canceled &#8211; Closed";
				}
				else if("eventType.promoteAllegation".equals(eventTypeCode)){
					status = "status.openCase";
					message = "Allegation Promoted to Case";
				}
				else if("eventType.createAllegation".equals(eventTypeCode)){
	 				status = "status.openAllegation";
	 				message = "Allegation Created";
	 			}
	 			else if("eventType.createCase".equals(eventTypeCode)){
	 				status = "status.openCase";
	 				message = "Case Created";
	 			}

				if(status != null){
					etk.getResult().addMessage(message);
					try {
						complaint.setCurrentStatus(RdoDao.getIdFromCode(etk, "T_STATUS", status));
					} catch (IncorrectResultSizeDataAccessException e) {
						e.printStackTrace();
						throw new ApplicationException(e);
					}
				}
				complaint.setLastEventDate(event.getStartDate());
				complaint.setDueDate(event.getDueDate());

				try {
					etk.getDynamicObjectService().createSaveOperation(complaint).setExecuteEvents(false).save();
				} catch (InputValidationException e) {
					e.printStackTrace();
					throw new ApplicationException(e);
				}
    		}
    	}

    }

}
