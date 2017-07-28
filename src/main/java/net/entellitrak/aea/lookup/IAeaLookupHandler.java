/**
 *
 * Additional lookup handler that AEs should implement. Necessary for RDO data import/export.
 *
 * alee 01/20/2015
 **/

package net.entellitrak.aea.lookup;

import com.entellitrak.ExecutionContext;

/**
 * Defines attributes of a script type lookup needed for efficient
 * RDO import/export.
 *
 * @author aclee
 *
 */
public interface IAeaLookupHandler {
    /**
     * The TABLE that contains the "as VALUE" column.
     *
     * Example: if the query is
     * "select ID as VALUE, NAME as DISPLAY from T_RDO_TABLE"
     * the implementation of this method should return "T_RDO_TABLE".
     * @param etk entellitrak execution context
     *
     * @return The TABLE that contains the "as VALUE" column.
     */
    String getValueTableName(ExecutionContext etk);

    /**
     * The actual name of the "as VALUE" column, ex... "ID".
     *
     * Example: if the query is
     * "select ID as VALUE, NAME as DISPLAY from T_RDO_TABLE"
     * the implementation of this method should return "ID".
     *
     *@param etk entellitrak execution context
     *
     * @return The actual name of the "as VALUE" column, ex... "ID".
     */
    String getValueColumnName(ExecutionContext etk);
}
