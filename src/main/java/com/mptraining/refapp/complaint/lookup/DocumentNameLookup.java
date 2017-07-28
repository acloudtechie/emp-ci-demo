/**
 *
 * Document Name Lookup
 *
 * administrator 09/29/2016
 **/

package com.mptraining.refapp.complaint.lookup;

import com.entellitrak.ApplicationException;
import com.entellitrak.lookup.LookupHandler;
import com.entellitrak.lookup.LookupExecutionContext;

public class DocumentNameLookup implements LookupHandler {

    @Override
	public String execute(LookupExecutionContext etk) throws ApplicationException {

    	if(etk.isForTracking()){
        	return "SELECT ID AS VALUE, C_NAME AS DISPLAY FROM T_DOCUMENT_NAME_TYPE " + 
        			"WHERE (ID = {?documentName}) " + 
        			"OR ((C_DOCUMENT_TYPE = {?documentType}) " + 
        			"AND ((ETK_START_DATE IS NULL OR TRUNC(SYSDATE) >= ETK_START_DATE) " +
					"AND (ETK_END_DATE IS NULL OR TRUNC(SYSDATE) <= ETK_END_DATE))) " +
					"ORDER BY C_ORDER";
        }
        else if(etk.isForView()){
        	return "SELECT ID AS VALUE, C_NAME AS DISPLAY FROM T_DOCUMENT_NAME_TYPE";
        }
        else if(etk.isForSearch() || etk.isForAdvancedSearch()){
        	return "SELECT ID AS VALUE, C_NAME AS DISPLAY FROM T_DOCUMENT_NAME_TYPE ORDER BY C_ORDER";
        }
        else if(etk.isForSingleResult()){
        	return "SELECT ID AS VALUE, C_NAME AS DISPLAY FROM T_DOCUMENT_NAME_TYPE WHERE ID = {?documentName}";
        }
        else{
        	throw new ApplicationException(this.getClass().getName() + " - non-configured isFor() context utilized.");
        }
    }

}
