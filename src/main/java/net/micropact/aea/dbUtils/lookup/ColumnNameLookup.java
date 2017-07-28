/**
 *
 * Looks up columns for a given table name.
 *
 * alee 12/19/2014
 **/

package net.micropact.aea.dbUtils.lookup;

import net.entellitrak.aea.lookup.IAeaLookupHandler;
import net.micropact.aea.utility.Utility;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.lookup.LookupHandler;
import com.entellitrak.lookup.LookupExecutionContext;

/**
 * This lookup handler can be used to select from a set of columns for a particular database table {?databaseTableName}.
 *
 * @author MicroPact
 */
public class ColumnNameLookup implements LookupHandler, IAeaLookupHandler {

    @Override
	public String execute(final LookupExecutionContext etk) throws ApplicationException {

    	final String infoTableName = (Utility.isSqlServer(etk) ? " INFORMATION_SCHEMA.COLUMNS " : " USER_TAB_COLUMNS ");
    	final String nvlFunction =   (Utility.isSqlServer(etk) ? " isnull({?databaseTableName}, '') " : " nvl({?databaseTableName}, '') ");

        if (etk.isForTracking()) {
        	return    " SELECT column_name as display, column_name as value FROM " + infoTableName
        			+ " WHERE table_name = "
        			+ nvlFunction
        			+ " order by column_name";
        } else if (etk.isForSearch() || etk.isForAdvancedSearch()) {
        	return    " SELECT distinct column_name as display, column_name as value FROM " + infoTableName
        			+ " order by column_name";
        } else {
        	return    " SELECT distinct column_name as display, column_name as value FROM " + infoTableName;
        }
    }

	@Override
	public String getValueTableName(ExecutionContext theExecutionContext) {
		return (Utility.isSqlServer(theExecutionContext) ? "INFORMATION_SCHEMA.COLUMNS" : "USER_TAB_COLUMNS");
	}

	@Override
	public String getValueColumnName(ExecutionContext theExecutionContext) {
		return "column_name";
	}
}
