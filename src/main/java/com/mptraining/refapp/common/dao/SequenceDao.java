/**
 *
 * DAO for performing operations with any sequences
 *
 * administrator 12/02/2016
 **/

package com.mptraining.refapp.common.dao;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;

public class SequenceDao {

	public static void dropComplaintIdSeq(ExecutionContext etk){
		etk.createSQL("DROP SEQUENCE COMPLAINT_ID_SEQ").execute();
	}

	public static void createComplaintIdSeq(ExecutionContext etk){
		etk.createSQL("CREATE SEQUENCE COMPLAINT_ID_SEQ START WITH 0 INCREMENT BY 1 MINVALUE 0 MAXVALUE 9999 CYCLE").execute();
	}

	public static Integer selectFromComplaintIdSeq(ExecutionContext etk){
		try {
			return etk.createSQL("SELECT COMPLAINT_ID_SEQ.NEXTVAL FROM DUAL ").returnEmptyResultSetAs(null).fetchInt();
		} catch (IncorrectResultSizeDataAccessException e) {
			//This should never happen and if the sequence doesn't exist a different error will be thrown
			return 0;
		}
	}

}
