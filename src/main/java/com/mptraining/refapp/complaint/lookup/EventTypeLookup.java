/**
 *
 * Event Type Lookup
 *
 * administrator 09/30/2016
 **/

package com.mptraining.refapp.complaint.lookup;

import com.entellitrak.ApplicationException;
import com.entellitrak.lookup.LookupExecutionContext;
import com.entellitrak.lookup.LookupHandler;

public class EventTypeLookup implements LookupHandler {

    @Override
	public String execute(LookupExecutionContext etk) throws ApplicationException {

    	if(etk.isForTracking()){
			return "SELECT ID AS VALUE, " +
					"  C_NAME  AS DISPLAY " +
					"FROM T_EVENT_TYPE " +
					"WHERE (C_CODE IN " +
					"  (SELECT 'eventType.accepted' " +
					"  FROM T_COMPLAINT com " +
					"  LEFT JOIN T_STATUS status " +
					"  ON status.id      = com.C_CURRENT_STATUS " +
					"  WHERE com.ID      = {?parentId} " +
					"  AND status.C_CODE = 'status.openAllegation' " +
					"  UNION " +
					"  SELECT 'eventType.promoteAllegation' " +
					"  FROM T_COMPLAINT com " +
					"  LEFT JOIN T_STATUS status " +
					"  ON status.id                                                                   = com.C_CURRENT_STATUS " +
					"  WHERE com.ID                                                                   = {?parentId} " +
					"  AND status.C_CODE                                                              = 'status.openAllegation' " +
					"  AND (SELECT BUSINESS_KEY FROM ETK_ROLE WHERE ROLE_ID = {?currentUser.roleId}) IN ('role.specialAgent','role.sac') " +
					"  UNION " +
					"  SELECT 'eventType.closeCase' " +
					"  FROM T_COMPLAINT com " +
					"  LEFT JOIN T_STATUS status " +
					"  ON status.id                                                                   = com.C_CURRENT_STATUS " +
					"  WHERE com.ID                                                                   = {?parentId} " +
					"  AND status.C_CODE                                                              = 'status.openCase' " +
					"  AND (SELECT BUSINESS_KEY FROM ETK_ROLE WHERE ROLE_ID = {?currentUser.roleId}) IN ('role.specialAgent','role.sac') " +
					"  UNION " +
					"  SELECT 'eventType.cancel' " +
					"  FROM T_COMPLAINT com " +
					"  LEFT JOIN T_STATUS status " +
					"  ON status.id                                                                  = com.C_CURRENT_STATUS " +
					"  WHERE com.ID                                                                  = {?parentId} " +
					"  AND status.C_CODE                                                             = 'status.openAllegation' " +
					"  AND (SELECT BUSINESS_KEY FROM ETK_ROLE WHERE ROLE_ID = {?currentUser.roleId}) = 'role.specialAgent' " +
					"  UNION " +
					"  SELECT 'eventType.conductPolygraph' " +
					"  FROM T_COMPLAINT com " +
					"  LEFT JOIN T_STATUS status " +
					"  ON status.id       = com.C_CURRENT_STATUS " +
					"  WHERE com.ID       = {?parentId} " +
					"  AND status.C_CODE IN ('status.openAllegation', 'status.openCase') " +
					"  UNION " +
					"  SELECT 'eventType.searchAndSeizure' " +
					"  FROM T_COMPLAINT com " +
					"  LEFT JOIN T_STATUS status " +
					"  ON status.id      = com.C_CURRENT_STATUS " +
					"  WHERE com.ID      = {?parentId} " +
					"  AND status.C_CODE = 'status.openCase' " +
					"  UNION " +
					"  SELECT 'eventType.newHotlineRequest' " +
					"  FROM T_COMPLAINT com " +
					"  LEFT JOIN T_STATUS status " +
					"  ON status.id       = com.C_CURRENT_STATUS " +
					"  WHERE com.ID       = {?parentId} " +
					"  AND status.C_CODE IN ('status.openAllegation', 'status.openCase') " +
					"  UNION " +
					"  SELECT 'eventType.reviewComplaint' " +
					"  FROM T_COMPLAINT com " +
					"  LEFT JOIN T_STATUS status " +
					"  ON status.id       = com.C_CURRENT_STATUS " +
					"  WHERE com.ID       = {?parentId} " +
					"  AND status.C_CODE IN ('status.openAllegation', 'status.openCase', 'status.closed') " +
					"  ) " +
					"AND ((ETK_START_DATE IS NULL " +
					"AND ETK_END_DATE     IS NULL) " +
					"OR (ETK_START_DATE   IS NULL " +
					"AND TRUNC(SYSDATE)   <= ETK_END_DATE) " +
					"OR (ETK_END_DATE     IS NULL " +
					"AND TRUNC(SYSDATE)   >= ETK_START_DATE) " +
					"OR (TRUNC(sysdate) BETWEEN ETK_START_DATE AND ETK_END_DATE) )) " +
					"OR ID = {?eventType} " +
					"ORDER BY C_NAME";
    	} else{
			return "SELECT ID AS VALUE, C_NAME AS DISPLAY FROM T_EVENT_TYPE";
		}

    }

}
