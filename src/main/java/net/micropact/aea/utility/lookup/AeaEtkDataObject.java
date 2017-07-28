package net.micropact.aea.utility.lookup;

import java.util.List;

import net.micropact.aea.utility.DataObjectType;

public class AeaEtkDataObject {
	Boolean isDocumentManagementObject;
	Boolean isSeperateInbox;
	Boolean isSearchable;
	Boolean isBaseObject;
	Boolean isDocumentManagementEnabled;
	Boolean isAppliedChanges;
	Boolean isAutoAssignment;
	DataObjectType dataObjectType;
	Integer listStyle;
	Integer cardinality;
	Integer listOrder;
	Integer designator;
	Long parentObjectId;
	Long trackingConfigId;
	Long dataObjectId;
	String objectName;
	String tableSpace;
	String name;
	String description;
	String label;
	String tableName;
	String businessKey;
	List<AeaEtkDataElement> dataElements;

	public Boolean getIsDocumentManagementObject() {
		return isDocumentManagementObject;
	}
	public void setIsDocumentManagementObject(final Boolean isDocumentManagementObject) {
		this.isDocumentManagementObject = isDocumentManagementObject;
	}
	public Boolean getIsSeperateInbox() {
		return isSeperateInbox;
	}
	public void setIsSeperateInbox(final Boolean isSeperateInbox) {
		this.isSeperateInbox = isSeperateInbox;
	}
	public Boolean getIsSearchable() {
		return isSearchable;
	}
	public void setIsSearchable(final Boolean isSearchable) {
		this.isSearchable = isSearchable;
	}
	public Boolean getIsBaseObject() {
		return isBaseObject;
	}
	public void setIsBaseObject(final Boolean isBaseObject) {
		this.isBaseObject = isBaseObject;
	}
	public Boolean getIsDocumentManagementEnabled() {
		return isDocumentManagementEnabled;
	}
	public void setIsDocumentManagementEnabled(final Boolean isDocumentManagementEnabled) {
		this.isDocumentManagementEnabled = isDocumentManagementEnabled;
	}
	public Boolean getIsAppliedChanges() {
		return isAppliedChanges;
	}
	public void setIsAppliedChanges(final Boolean isAppliedChanges) {
		this.isAppliedChanges = isAppliedChanges;
	}
	public DataObjectType getDataObjectType() {
		return dataObjectType;
	}
	public void setDataObjectType(final DataObjectType dataObjectType) {
		this.dataObjectType = dataObjectType;
	}
	public Integer getListStyle() {
		return listStyle;
	}
	public void setListStyle(final Integer listStyle) {
		this.listStyle = listStyle;
	}
	public Integer getCardinality() {
		return cardinality;
	}
	public void setCardinality(final Integer cardinality) {
		this.cardinality = cardinality;
	}
	public Integer getListOrder() {
		return listOrder;
	}
	public void setListOrder(final Integer listOrder) {
		this.listOrder = listOrder;
	}
	public Long getParentObjectId() {
		return parentObjectId;
	}
	public void setParentObjectId(final Long parentObjectId) {
		this.parentObjectId = parentObjectId;
	}
	public Long getTrackingConfigId() {
		return trackingConfigId;
	}
	public void setTrackingConfigId(final Long trackingConfigId) {
		this.trackingConfigId = trackingConfigId;
	}
	public Long getDataObjectId() {
		return dataObjectId;
	}
	public void setDataObjectId(final Long dataObjectId) {
		this.dataObjectId = dataObjectId;
	}
	public String getObjectName() {
		return objectName;
	}
	public void setObjectName(final String objectName) {
		this.objectName = objectName;
	}
	public String getTableSpace() {
		return tableSpace;
	}
	public void setTableSpace(final String tableSpace) {
		this.tableSpace = tableSpace;
	}
	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(final String description) {
		this.description = description;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(final String label) {
		this.label = label;
	}
	public String getTableName() {
		return tableName;
	}
	public void setTableName(final String tableName) {
		this.tableName = tableName;
	}
	public String getBusinessKey() {
		return businessKey;
	}
	public void setBusinessKey(final String businessKey) {
		this.businessKey = businessKey;
	}
	public List<AeaEtkDataElement> getDataElements() {
		return dataElements;
	}
	public void setDataElements(final List<AeaEtkDataElement> dataElements) {
		this.dataElements = dataElements;
	}
	public Boolean getIsAutoAssignment() {
		return isAutoAssignment;
	}
	public void setIsAutoAssignment(Boolean aIsAutoAssignment) {
		isAutoAssignment = aIsAutoAssignment;
	}
	public Integer getDesignator() {
		return designator;
	}
	public void setDesignator(Integer aDesignator) {
		designator = aDesignator;
	}
}
