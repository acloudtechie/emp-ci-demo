package net.micropact.aea.dbUtils.page.rdodataexport;

import java.io.Serializable;

/**
 * An RDO table definition to save selected RDO tables.
 *
 * @author MicroPact
 *
 */
public class RdoTable implements Comparable<RdoTable>, Serializable {

	/**
	 * Serializable RDO Table UID.
	 */
	private static final long serialVersionUID = 5620341132831451940L;
	private String tableName;
	private int sortOrder;


	public String getTableName() {
		return tableName;
	}

	public void setTableName(final String aTableName) {
		this.tableName = aTableName;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(final int aSortOrder) {
		this.sortOrder = aSortOrder;
	}


	@Override
	public int compareTo(final RdoTable secondTable) {
		if (this.sortOrder == secondTable.sortOrder) {
			return 0;
		} else if (this.sortOrder > secondTable.sortOrder) {
			return 1;
		} else {
			return -1;
		}
	}

}
