/**
 *
 * Returns all table names in the system.
 *
 * alee 12/19/2014
 **/

package net.micropact.aea.dbUtils.lookup;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.lookup.LookupExecutionContext;
import com.entellitrak.lookup.LookupHandler;

import net.entellitrak.aea.lookup.IAeaLookupHandler;
import net.micropact.aea.utility.Utility;

/**
 * This lookup handler returns a list of all tables in the database.
 *
 * @author MicroPact
 */
public class TableNameLookup implements LookupHandler, IAeaLookupHandler {

    @Override
    public String execute(final LookupExecutionContext etk) throws ApplicationException {

        final String infoTableName = (Utility.isSqlServer(etk) ? " INFORMATION_SCHEMA.COLUMNS " : " USER_TAB_COLUMNS ");


        if (etk.isForTracking() || etk.isForSearch() || etk.isForAdvancedSearch()) {
            return " SELECT distinct table_name as DISPLAY, table_name as VALUE "
                    + " FROM "
                    + infoTableName
                    + " order by table_name";
        } else {
            return " SELECT distinct table_name as DISPLAY, table_name as VALUE FROM " + infoTableName;
        }
    }

    @Override
    public String getValueTableName(ExecutionContext theExecutionContext) {
        return (Utility.isSqlServer(theExecutionContext) ? "INFORMATION_SCHEMA.COLUMNS" : "USER_TAB_COLUMNS");
    }

    @Override
    public String getValueColumnName(ExecutionContext theExecutionContext) {
        return "table_name";
    }
}
