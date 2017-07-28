/**
 *
 * Complaint Type Lookup
 *
 * administrator 09/29/2016
 **/

package com.mptraining.refapp.complaint.lookup;

import com.entellitrak.ApplicationException;
import com.entellitrak.lookup.LookupHandler;
import com.entellitrak.lookup.LookupExecutionContext;

public class ComplaintTypeLookup implements LookupHandler {

    @Override
	public String execute(LookupExecutionContext etk) throws ApplicationException {

        if(etk.isForTracking()){
        	return "SELECT ID AS VALUE, C_NAME AS DISPLAY FROM T_COMPLAINT_TYPE " + 
        			"WHERE (ID = {?complaintType}) " +
        			"OR ((ID IN (SELECT ID_OWNER FROM M_TYPE_CATEGORY WHERE C_CATEGORY = {?category} )) " + 
        			"AND ((ETK_START_DATE IS NULL OR TRUNC(SYSDATE) >= ETK_START_DATE) " +
					"AND (ETK_END_DATE IS NULL OR TRUNC(SYSDATE) <= ETK_END_DATE))) " +
					"ORDER BY C_ORDER";
        }
        else if(etk.isForView()){
        	return "SELECT ID AS VALUE, C_NAME AS DISPLAY FROM T_COMPLAINT_TYPE";
        }
        else if(etk.isForSearch() || etk.isForAdvancedSearch()){
        	return "SELECT ID AS VALUE, C_NAME AS DISPLAY FROM T_COMPLAINT_TYPE ORDER BY C_ORDER";
        }
        else if(etk.isForSingleResult()){
        	return "SELECT ID AS VALUE, C_NAME AS DISPLAY FROM T_COMPLAINT_TYPE WHERE ID = {?complaintType}";
        }
        else{
        	throw new ApplicationException(this.getClass().getName() + " - non-configured isFor() context utilized.");
        }
    }

}
