package net.entellitrak.aea.auditLog;

import com.entellitrak.user.User;

import net.micropact.aea.auditLog.utility.LogEventType;
import net.micropact.aea.auditLog.utility.UserSystemEventType;

/**
 * Defines elements needed to manually create an AEA Audit Log entry.
 *
 * @author MicroPact
 *
 */
public class AeaLogEntry {

	private Long baseId;
	private Long parentId;
	private Long trackingId;
	private LogEventType crudEventType;
	private UserSystemEventType userSystemEventType;
	private User user;
	private String tableName;
	private String columnName;
	private String dataObjectName;
	private String dataObjectLabel;
	private String dataObjectKey;
	private String dataElementName;
	private String previousDataValue;
	private String newDataValue;
	private String message;

	public Long getBaseId() {
		return baseId;
	}
	public void setBaseId(final Long aBaseId) {
		this.baseId = aBaseId;
	}
	public Long getParentId() {
		return parentId;
	}
	public void setParentId(final Long aParentId) {
		this.parentId = aParentId;
	}
	public Long getTrackingId() {
		return trackingId;
	}
	public void setTrackingId(final Long aTrackingId) {
		this.trackingId = aTrackingId;
	}
	public LogEventType getCrudEventType() {
		return crudEventType;
	}
	public void setCrudEventType(final LogEventType aCrudEventType) {
		this.crudEventType = aCrudEventType;
	}
	public UserSystemEventType getUserSystemEventType() {
		return userSystemEventType;
	}
	public void setUserSystemEventType(final UserSystemEventType aUserSystemEventType) {
		this.userSystemEventType = aUserSystemEventType;
	}
	public String getTableName() {
		return tableName;
	}
	public void setTableName(final String aTableName) {
		this.tableName = aTableName;
	}
	public String getColumnName() {
		return columnName;
	}
	public void setColumnName(final String aColumnName) {
		this.columnName = aColumnName;
	}
	public String getDataObjectName() {
		return dataObjectName;
	}
	public void setDataObjectName(final String aDataObjectName) {
		this.dataObjectName = aDataObjectName;
	}
	public String getDataElementName() {
		return dataElementName;
	}
	public void setDataElementName(final String aDataElementName) {
		this.dataElementName = aDataElementName;
	}
	public String getPreviousDataValue() {
		return previousDataValue;
	}
	public void setPreviousDataValue(final String aPreviousDataValue) {
		this.previousDataValue = aPreviousDataValue;
	}
	public String getNewDataValue() {
		return newDataValue;
	}
	public void setNewDataValue(final String aNewDataValue) {
		this.newDataValue = aNewDataValue;
	}
	public User getUser() {
		return user;
	}
	public void setUser(final User aUser) {
		this.user = aUser;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(final String aMessage) {
		this.message = aMessage;
	}
	public String getDataObjectLabel() {
		return dataObjectLabel;
	}
	public void setDataObjectLabel(final String aDataObjectLabel) {
		this.dataObjectLabel = aDataObjectLabel;
	}
	public String getDataObjectKey() {
		return dataObjectKey;
	}
	public void setDataObjectKey(final String aDataObjectKey) {
		this.dataObjectKey = aDataObjectKey;
	}
}