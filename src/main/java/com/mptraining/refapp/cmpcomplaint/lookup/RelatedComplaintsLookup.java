/**
 *
 * Lookup of all CMP Complaints
 *
 * administrator 01/05/2017
 **/

package com.mptraining.refapp.cmpcomplaint.lookup;

import com.entellitrak.ApplicationException;
import com.entellitrak.lookup.LookupHandler;
import com.entellitrak.lookup.LookupExecutionContext;

public class RelatedComplaintsLookup implements LookupHandler {

    public String execute(LookupExecutionContext etk) throws ApplicationException {

    	if(etk.isForTracking()){
    		return "SELECT ID AS VALUE, C_COMPLAINT_ID as DISPLAY, C_SHORT_TITLE as short_title FROM T_CMP_COMPLAINT WHERE C_COMPLAINT_ID IS NOT NULL AND ( ID IN ({?relatedComplaints}) OR '{$isLiveSearchAjax}' = '1')";
    	}
    	else if(etk.isForView()){
        	return "SELECT ID AS VALUE, C_COMPLAINT_ID as DISPLAY FROM T_CMP_COMPLAINT WHERE C_COMPLAINT_ID IS NOT NULL";
        }
        else if(etk.isForSearch() || etk.isForAdvancedSearch()){
        	return "SELECT ID AS VALUE, C_COMPLAINT_ID as DISPLAY FROM T_CMP_COMPLAINT WHERE C_COMPLAINT_ID IS NOT NULL ORDER BY C_COMPLAINT_ID";
        }
        else if(etk.isForSingleResult()){
        	return "SELECT ID AS VALUE, C_COMPLAINT_ID as DISPLAY FROM T_CMP_COMPLAINT WHERE C_COMPLAINT_ID IS NOT NULL AND ID IN ({?relatedComplaints})";
        }
        else{
        	throw new ApplicationException(this.getClass().getName() + " - non-configured isFor() context utilized.");
        }
    }

}
