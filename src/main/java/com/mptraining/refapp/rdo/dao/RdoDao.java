/**
 *
 * DAO for accessing information about Reference Data.
 *
 * administrator 12/01/2016
 **/

package com.mptraining.refapp.rdo.dao;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;

public class RdoDao {

	public static Integer getIdFromCode(ExecutionContext etk, String table, String code) throws IncorrectResultSizeDataAccessException{
		return etk.createSQL("SELECT ID FROM " + table + " WHERE C_CODE = :code").setParameter("code", code).returnEmptyResultSetAs(null).fetchInt();
	}
}
