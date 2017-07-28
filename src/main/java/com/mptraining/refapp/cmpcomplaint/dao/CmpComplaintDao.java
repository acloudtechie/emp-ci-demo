/**
 *
 * Handles SQL calls regarding the CmpComplaint object
 *
 * administrator 12/05/2016
 **/

package com.mptraining.refapp.cmpcomplaint.dao;

import java.util.List;
import java.util.Map;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;

public class CmpComplaintDao {

	public static void clearMessages(ExecutionContext etk){
		etk.createSQL("UPDATE T_CMP_COMPLAINT SET C_MESSAGE = '' ").execute();
	}

	public static void setPastDueEventMessages(ExecutionContext etk){
		etk.createSQL("UPDATE T_CMP_COMPLAINT " +
				"SET C_MESSAGE = 'This complaint has a past due event.' " +
				"WHERE ID     IN " +
				"  ( SELECT DISTINCT complaint.ID " +
				"  FROM T_CMP_EVENT event " +
				"  LEFT JOIN T_CMP_COMPLAINT complaint " +
				"  ON complaint.ID = event.ID_BASE " +
				"  LEFT JOIN T_RF_STATE status " +
				"  ON status.ID                = complaint.C_CURRENT_STATUS " +
				"  WHERE event.C_DUE_DATE      < SYSDATE " +
				"  AND event.C_COMPLETED_DATE IS NULL " +
				"  AND (status.C_CODE          = 'state.openCase' " +
				"  OR status.C_CODE            = 'state.openAllegation') " +
				"  )").execute();
	}

	public static List<Map<String, Object>> getApproachingDueEventInfo(ExecutionContext etk) {
		return etk.createSQL("SELECT DISTINCT complaint.ID as COMP_ID, event.ID as EVENT_ID, " +
    			"  eu.EMAIL_ADDRESS, " +
    			"  et.C_NAME as EVENT_TYPE " +
    			"FROM T_CMP_EVENT event " +
    			"LEFT JOIN T_CMP_COMPLAINT complaint " +
    			"ON complaint.ID = event.ID_BASE " +
    			"LEFT JOIN T_RF_STATE status " +
    			"ON status.ID = complaint.C_CURRENT_STATUS " +
    			"LEFT JOIN ETK_USER eu " +
    			"ON eu.USER_ID = complaint.C_SAC_ASSIGNED " +
    			"LEFT JOIN T_RF_TRANSITION et " +
    			"ON et.ID                        = event.C_EVENT_TYPE " +
    			"WHERE complaint.C_SAC_ASSIGNED IS NOT NULL " +
    			"AND TRUNC(event.C_DUE_DATE)-3 = TRUNC(SYSDATE) " +
    			"AND event.C_COMPLETED_DATE     IS NULL " +
    			"AND (status.C_CODE              = 'state.openCase' " +
    			"OR status.C_CODE                = 'state.openAllegation')").fetchList();
	}

	public static Integer countOpenComplaintsAssignedToSac(ExecutionContext etk, Long sacId) throws IncorrectResultSizeDataAccessException{
		return etk.createSQL("SELECT COUNT(0) FROM T_CMP_COMPLAINT comp LEFT JOIN T_RF_STATE status ON status.id = comp.C_CURRENT_STATUS WHERE C_SAC_ASSIGNED = :sacId AND status.C_CODE IN ('state.openAllegation', 'state.openCase') ")
			.setParameter("sacId", sacId)
			.fetchInt();
	}
}
