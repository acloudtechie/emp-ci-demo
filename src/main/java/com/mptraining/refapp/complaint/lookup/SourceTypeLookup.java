/**
 *
 * Source Type Lookup
 *
 * administrator 09/30/2016
 **/

package com.mptraining.refapp.complaint.lookup;

import com.entellitrak.ApplicationException;
import com.entellitrak.lookup.LookupHandler;
import com.entellitrak.lookup.LookupExecutionContext;

public class SourceTypeLookup implements LookupHandler {

    @Override
	public String execute(LookupExecutionContext etk) throws ApplicationException {

    	if(etk.isForTracking() && "role.efiler".equals(etk.getCurrentUser().getRole().getBusinessKey())){
        	return "SELECT ID AS VALUE, C_NAME AS DISPLAY FROM T_SOURCE_TYPE WHERE C_CODE = 'sourceType.efiler' ";
        }else if(etk.isForTracking()){
        	return "SELECT ID AS VALUE, C_NAME AS DISPLAY FROM T_SOURCE_TYPE " + 
        			"WHERE (ID = {?source}) " + 
        			"OR ((ID IN (SELECT ID_OWNER FROM M_SOURCE_CATEGORY WHERE C_CATEGORY = {?category} ))  " + 
        			"AND ((ETK_START_DATE IS NULL OR TRUNC(SYSDATE) >= ETK_START_DATE) " +
					"AND (ETK_END_DATE IS NULL OR TRUNC(SYSDATE) <= ETK_END_DATE))) " +
					"ORDER BY C_ORDER";
        }
        else if(etk.isForView()){
        	return "SELECT ID AS VALUE, C_NAME AS DISPLAY FROM T_SOURCE_TYPE";
        }
        else if(etk.isForSearch() || etk.isForAdvancedSearch()){
        	return "SELECT ID AS VALUE, C_NAME AS DISPLAY FROM T_SOURCE_TYPE ORDER BY C_ORDER";
        }
        else if(etk.isForSingleResult()){
        	return "SELECT ID AS VALUE, C_NAME AS DISPLAY FROM T_SOURCE_TYPE WHERE ID = {?source}";
        }
        else{
        	throw new ApplicationException(this.getClass().getName() + " - non-configured isFor() context utilized.");
        }
    }

}
