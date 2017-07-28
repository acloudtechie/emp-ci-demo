package net.micropact.aea.auditLog.utility;

/**
 * Defines columns to be excluded from the Audit Log.
 *
 * @author MicroPact
 */
public class ExcludedColumn {
	private final String tableName;
	private final String columnName;

	/**
	 * Excludes a column from the Audit Log.
	 *
	 * @param aTableName Table name to exclude.
	 * @param aColumnName Column name to exclude.
	 */
	public ExcludedColumn(final String aTableName, final String aColumnName) {
		this.tableName = aTableName;
		this.columnName = aColumnName;
	}

	public String getTableName() {
		return tableName;
	}

	public String getColumnName() {
		return columnName;
	}
}
