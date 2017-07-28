/**
 *
 * Lookup which filters the Category to just Allegation if the user is an eFiler
 *
 * administrator 10/12/2016
 **/

package com.mptraining.refapp.common.lookup;

import com.entellitrak.ApplicationException;
import com.entellitrak.lookup.LookupHandler;
import com.entellitrak.lookup.LookupExecutionContext;

public class CategoryLookup implements LookupHandler {

    @Override
	public String execute(LookupExecutionContext etk) throws ApplicationException {

        if(etk.isForTracking() && "role.efiler".equals(etk.getCurrentUser().getRole().getBusinessKey())){
        	return "SELECT ID AS VALUE, C_NAME AS DISPLAY FROM T_CATEGORY_TYPE WHERE C_CODE = 'categoryType.allegation' ";
        }
        else if(etk.isForTracking()){
        	return "SELECT ID AS VALUE, C_NAME AS DISPLAY FROM T_CATEGORY_TYPE  " + 
        			"WHERE (ID = {?category}) " + 
        			"OR ((ETK_START_DATE IS NULL OR TRUNC(SYSDATE) >= ETK_START_DATE) " +
					"AND (ETK_END_DATE IS NULL OR TRUNC(SYSDATE) <= ETK_END_DATE)) " +
					"ORDER BY C_ORDER";
        }
        else if(etk.isForView()){
        	return "SELECT ID AS VALUE, C_NAME AS DISPLAY FROM T_CATEGORY_TYPE";
        }
        else if(etk.isForSearch() || etk.isForAdvancedSearch()){
        	return "SELECT ID AS VALUE, C_NAME AS DISPLAY FROM T_CATEGORY_TYPE ORDER BY C_ORDER";
        }
        else if(etk.isForSingleResult()){
        	return "SELECT ID AS VALUE, C_NAME AS DISPLAY FROM T_CATEGORY_TYPE WHERE ID = {?category}";
        }
        else{
        	throw new ApplicationException(this.getClass().getName() + " - non-configured isFor() context utilized.");
        }
        
    }

}
