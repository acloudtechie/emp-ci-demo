package net.micropact.aea.utility.rdoutils;

import net.micropact.aea.utility.DataElementType;

/**
 * This element defines a Data Element (either for a Form or View).
 *
 * @author aclee
 *
 */
public class RdoDataElement {
	String name;
	String label;
	DataElementType dataType;
	String formControlType;
	String mTableName;
	String columnName;
	String lookupBusinessKey;

	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(final String label) {
		this.label = label;
	}
	public DataElementType getDataType() {
		return dataType;
	}
	public void setDataType(final DataElementType dataType) {
		this.dataType = dataType;
	}
	public String getFormControlType() {
		return formControlType;
	}
	public void setFormControlType(final String formControlType) {
		this.formControlType = formControlType;
	}
	public String getMTableName() {
		return mTableName;
	}
	public void setMTableName(final String tableName) {
		this.mTableName = tableName;
	}
	public String getColumnName() {
		return columnName;
	}
	public void setColumnName(final String columnName) {
		this.columnName = columnName;
	}
	public String getLookupBusinessKey() {
		return lookupBusinessKey;
	}
	public void setLookupBusinessKey(final String lookupBusinessKey) {
		this.lookupBusinessKey = lookupBusinessKey;
	}
}
