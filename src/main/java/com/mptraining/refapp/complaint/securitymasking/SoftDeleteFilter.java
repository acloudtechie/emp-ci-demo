/**
 *
 * Filters out soft deleted records from an efiler's view.
 *
 * administrator 12/13/2016
**/

package com.mptraining.refapp.complaint.securitymasking;

import com.entellitrak.ApplicationException;
import com.entellitrak.configuration.DataElement;
import com.entellitrak.configuration.DataObject;
import com.entellitrak.filter.Criteria;
import com.entellitrak.filter.FilterExecutionContext;
import com.entellitrak.filter.RecordFilterHandler;
import com.entellitrak.security.SecurityAuditFilterAction;

public class SoftDeleteFilter implements RecordFilterHandler {

    public void configureCriteria(FilterExecutionContext etk, Criteria criteria) throws ApplicationException {
    	
    	//Don't allow eFilers to see records that have been "soft deleted".  i.e. where the Deleted field is true.
    	if("role.efiler".equals(etk.getCurrentUser().getRole().getBusinessKey())){
    		DataObject complaint = etk.getDataObject();
    		criteria.add(
    	            criteria.isNull(complaint.getElement("object.cmpComplaint.element.deleted"))
    	        );
    	}
    	
    	//Log attempts to directly access filtered records 
    	//or searches who's criteria which specifically trigger the filter criteria
    	SecurityAuditFilterAction fa = new SecurityAuditFilterAction();
    	auditAllFields(fa, etk.getDataObject());
    	criteria.actions(fa);
    }
    
    //Recursively log all fields for an object and all its children
    private void auditAllFields(SecurityAuditFilterAction fa, DataObject dataObject) {
    	if (dataObject == null) {
    		return;
    	}

    	for (DataElement de : dataObject.getElements()) {
    		fa.getElements().add(de);
    	}

    	if (dataObject.getChildren() != null) {
    		for (DataObject aChild : dataObject.getChildren()) {
    			auditAllFields(fa, aChild);
    		}
    	}
    }
}
