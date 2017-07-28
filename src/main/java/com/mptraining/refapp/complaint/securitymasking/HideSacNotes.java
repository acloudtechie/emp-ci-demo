/**
 *
 * Only show the SAC Notes field when the user is SAC and the complaint is Open
 *
 * administrator 12/16/2016
**/

package com.mptraining.refapp.complaint.securitymasking;

import java.util.HashSet;
import java.util.Set;

import com.entellitrak.ApplicationException;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.configuration.DataElement;
import com.entellitrak.dynamic.CmpComplaint;
import com.entellitrak.dynamic.CmpEvent;
import com.entellitrak.filter.ElementFilterExecutionContext;
import com.entellitrak.filter.ElementFilterHandler;
import com.entellitrak.filter.FilterContextType;
import com.mptraining.refapp.rdo.dao.RdoDao;

public class HideSacNotes implements ElementFilterHandler<CmpEvent> {

    public Set<DataElement> getFilteredFields(ElementFilterExecutionContext<CmpEvent> etk) throws ApplicationException {
    	Set<DataElement> restrictedElements = new HashSet<DataElement>();
    	CmpEvent event = etk.getCurrentObjectInstance();
    	CmpComplaint complaint = etk.getDynamicObjectService().get(CmpComplaint.class, event.properties().getBaseId());
    	
    	//Get the RDO ID for an open complaint statuses. Use caching architecture to cut down on overhead - avoid
		//SQL statements that get executed on every execution of a restriction or masking handler wherever possible.
		Integer openCaseStateId = (Integer) etk.getCache().load("openCaseStateId");
		if (openCaseStateId == null) {
			try {
				openCaseStateId = RdoDao.getIdFromCode(etk, "T_RF_STATE", "state.openCase");
				etk.getCache().store("openCaseStateId", openCaseStateId);
			} catch (IncorrectResultSizeDataAccessException e) {
				throw new ApplicationException(e);
			}
		}
		Integer openAllegationStateId = (Integer) etk.getCache().load("openAllegationStateId");
		if (openAllegationStateId == null) {
			try {
				openAllegationStateId = RdoDao.getIdFromCode(etk, "T_RF_STATE", "state.openAllegation");
				etk.getCache().store("openAllegationStateId", openAllegationStateId);
			} catch (IncorrectResultSizeDataAccessException e) {
				throw new ApplicationException(e);
			}
		}
    	
    	if(etk.getFilterContextType() == FilterContextType.FORM && 
    			!("role.sac".equals(etk.getCurrentUser().getRole().getBusinessKey()) 
    			&& (complaint.getCurrentStatus().equals(openCaseStateId) || 
    					complaint.getCurrentStatus().equals(openAllegationStateId)))){
    		restrictedElements.add(event.configuration().getElement("object.cmpEvent.element.sacNotes"));
    	}
        return restrictedElements;
    }
}
