package net.micropact.aea.utility.lookup;

public class LookupTable {
	private String tableName;
	private String uniqueIdentifierColumn;
	private String valueColumn;

	public LookupTable (final String aTableName, final String aUniqueIdentifierColumn, final String aValueColumn) {
		this.tableName = aTableName;
		this.uniqueIdentifierColumn = aUniqueIdentifierColumn;
		this.valueColumn = aValueColumn;
	}

	public String getTableName() {
		return tableName;
	}
	public void setTableName(final String tableName) {
		this.tableName = tableName;
	}
	public String getUniqueIdentifierColumn() {
		return uniqueIdentifierColumn;
	}
	public void setUniqueIdentifierColumn(final String uniqueIdentifierColumn) {
		this.uniqueIdentifierColumn = uniqueIdentifierColumn;
	}
	public String getValueColumn() {
		return valueColumn;
	}
	public void setValueColumn(final String valueColumn) {
		this.valueColumn = valueColumn;
	}
}
