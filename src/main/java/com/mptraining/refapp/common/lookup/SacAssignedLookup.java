/**
 *
 * User lookup for all users with the SAC role.
 *
 * administrator 10/18/2016
 **/

package com.mptraining.refapp.common.lookup;

import com.entellitrak.ApplicationException;
import com.entellitrak.lookup.LookupHandler;
import com.entellitrak.lookup.LookupExecutionContext;

public class SacAssignedLookup implements LookupHandler {

    @Override
	public String execute(LookupExecutionContext etk) throws ApplicationException {

    	if(etk.isForTracking() || etk.isForSearch() || etk.isForAdvancedSearch()){
        	return "SELECT USER_ID AS VALUE, " +
            		"  EP.LAST_NAME " +
            		"  || ', ' " +
            		"  || EP.FIRST_NAME AS DISPLAY " +
            		"FROM ETK_USER eu " +
            		"LEFT JOIN ETK_SUBJECT_ROLE esr " +
            		"ON ESR.SUBJECT_ID = EU.USER_ID " +
            		"LEFT JOIN ETK_ROLE er " +
            		"ON ER.ROLE_ID = ESR.ROLE_ID " +
            		"LEFT JOIN ETK_PERSON ep " +
            		"ON EP.PERSON_ID       = eu.PERSON_ID " +
            		"WHERE ER.BUSINESS_KEY = 'role.sac' " +
            		"ORDER BY EP.LAST_NAME, EP.FIRST_NAME";
        }
    	else if(etk.isForView()){
        	return "SELECT USER_ID AS VALUE, " +
            		"  EP.LAST_NAME " +
            		"  || ', ' " +
            		"  || EP.FIRST_NAME AS DISPLAY " +
            		"FROM ETK_USER eu " +
            		"LEFT JOIN ETK_SUBJECT_ROLE esr " +
            		"ON ESR.SUBJECT_ID = EU.USER_ID " +
            		"LEFT JOIN ETK_ROLE er " +
            		"ON ER.ROLE_ID = ESR.ROLE_ID " +
            		"LEFT JOIN ETK_PERSON ep " +
            		"ON EP.PERSON_ID       = eu.PERSON_ID " +
            		"WHERE ER.BUSINESS_KEY = 'role.sac' ";
        }
        else if(etk.isForSingleResult()){
        	return "SELECT USER_ID AS VALUE, " +
            		"  EP.LAST_NAME " +
            		"  || ', ' " +
            		"  || EP.FIRST_NAME AS DISPLAY " +
            		"FROM ETK_USER eu " +
            		"LEFT JOIN ETK_PERSON ep " +
            		"ON EP.PERSON_ID       = eu.PERSON_ID " +
            		"WHERE USER_ID = {?sacAssigned} ";
        }
        else{
        	throw new ApplicationException(this.getClass().getName() + " - non-configured isFor() context utilized.");
        }
    }

}
