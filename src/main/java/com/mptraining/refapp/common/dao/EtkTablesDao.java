/**
 *
 * Contains methods for accessing core ETK tables
 *
 * administrator 12/05/2016
 **/

package com.mptraining.refapp.common.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;

public class EtkTablesDao {

	public static List<String> getGroupEmails(ExecutionContext etk, String groupBusinessKey){
		List<String> emails = new ArrayList<String>();
		List<Map<String,Object>> emailsInfo = etk.createSQL("SELECT EMAIL_ADDRESS AS EMAIL " +
				"FROM ETK_USER eu " +
				"LEFT JOIN ETK_USER_GROUP_ASSOC euga " +
				"ON eu.USER_ID = euga.USER_ID " +
				"LEFT JOIN ETK_GROUP eg " +
				"ON eg.GROUP_ID     = euga.GROUP_ID " +
				"WHERE BUSINESS_KEY = :group " +
				"AND EMAIL_ADDRESS IS NOT NULL").setParameter("group", groupBusinessKey).fetchList();
		for (Map<String,Object> email : emailsInfo) {
			emails.add(email.get("EMAIL").toString());
		}
		return emails;
	}

	public static Long getGroupId(ExecutionContext etk, String groupKey) throws ApplicationException{
		try {
			return etk.createSQL("SELECT GROUP_ID FROM ETK_GROUP WHERE BUSINESS_KEY = :key")
					.setParameter("key", groupKey)
					.fetchInt().longValue();
		} catch (IncorrectResultSizeDataAccessException e) {
			e.printStackTrace();
			throw new ApplicationException(e);
		}
	}
	
	public static void addUserToGroup(ExecutionContext etk, String groupBusinessKey, Long userId){
		etk.createSQL("INSERT INTO ETK_USER_GROUP_ASSOC (GROUP_ID, USER_ID) VALUES((SELECT GROUP_ID FROM ETK_GROUP WHERE BUSINESS_KEY = :group),:userId)")
	    	.setParameter("userId", userId)
	    	.setParameter("group", groupBusinessKey)
	    	.execute();
	}
	
	public static void updateScriptCode(ExecutionContext etk, String scriptId, String newCode){
		etk.createSQL("UPDATE ETK_SCRIPT_OBJECT SET CODE = :newCode WHERE SCRIPT_ID = :id")
			.setParameter("id", scriptId)
			.setParameter("newCode", newCode)
			.execute();
	}

	public static Integer getReportIdByBusinessKey(ExecutionContext etk, String reportBusinessKey) throws ApplicationException {
		try {
			return etk.createSQL("SELECT SAVED_REPORT_ID FROM ETK_SAVED_REPORT WHERE BUSINESS_KEY = :key")
					.setParameter("key", reportBusinessKey)
					.fetchInt();
		} catch (IncorrectResultSizeDataAccessException e) {
			e.printStackTrace();
			throw new ApplicationException(e);
		}
	}

}
