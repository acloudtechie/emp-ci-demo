package net.entellitrak.aea.auditLog;

import java.util.List;

import net.micropact.aea.auditLog.utility.ExcludedColumn;

/**
 * Configuration parameters for a new AEA Audit Log instance.
 *
 * @author MicroPact
 *
 */
public interface IAeaAuditLogConfig {

	/**
	 * Returns whether or not to store a message in the C_MESSAGE field for all CRUD log entries.
	 * System default is Boolean.TRUE, do not return a null value.
	 *
	 * @return Whether or not to store a message in the C_MESSAGE field for all CRUD log entries.
	 */
	Boolean logMessagesForCRUD();

	/**
	 * Returns the name of the table to store log entries in.
	 * NOTE: If CTO table, CTO must be a first level child to the BTO.
	 *
	 * System default is T_AEA_AUDIT_LOG, do not return a null value.
	 *
	 * @return The name of the table to store log entries in.
	 */
	String getLogTableName();

	/**
	 * *** NOT YET IMPLEMENTED *** RDO Only for the time being.
	 *
	 * Indicates whether the configured getLogTableName() is a CTO. If false, RDO is assumed.
	 * NOTE: If CTO table, CTO must be a first level child to the BTO.
	 *
	 * System default is Boolean.FALSE, do not return a null value.
	 *
	 * @return Indicates whether the configured getLogTableName() is a CTO. If false, RDO is assumed.
	 */
	Boolean isCTOLogTable();

	/**
	 * A list of columns to exclude from audit logging.
	 *
	 * System default is new ArrayList(), do not return a null value.
	 *
	 * @return A list of excluded columns (and their associated tables) to exclude from audit logging.
	 */
	List<ExcludedColumn> getExcludedColumns();
}
