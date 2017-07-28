package net.micropact.aea.utility.lookup;

import net.micropact.aea.utility.DataElementType;

public class AeaEtkDataElement {

	Boolean isBoundToLookup;
	Boolean isStoredInDocumentManagement;
	Boolean isAppliedChanges;
	Boolean isLogged;
	Boolean isIdentifier;
	Boolean areFutureDatesAllowed;
	Boolean isDefaultToToday;
	Boolean isUsedForEscan;
	Boolean isUnique;
	Boolean isSearchable;
	Boolean isSystemField;
	Boolean isPrimaryKey;
	Boolean isValidationRequired;
	Integer indexType;
	Integer dataSize;
	Long dataElementId;
	Long pluginRegistrationId;
	String elementName;
	String required;
	String defaultValue;
	String description;
	String name;
	String mTableName;
	String columnName;
	String businessKey;

	AeaEtkDataObject etkDataObject;
	AeaEtkLookupDefinition etkLookupDefinition;
	DataElementType dataType;


	public Boolean getIsBoundToLookup() {
		return isBoundToLookup;
	}
	public void setIsBoundToLookup(final Boolean isBoundToLookup) {
		this.isBoundToLookup = isBoundToLookup;
	}
	public Boolean getIsStoredInDocumentManagement() {
		return isStoredInDocumentManagement;
	}
	public void setIsStoredInDocumentManagement(final Boolean isStoredInDocumentManagement) {
		this.isStoredInDocumentManagement = isStoredInDocumentManagement;
	}
	public Boolean getIsAppliedChanges() {
		return isAppliedChanges;
	}
	public void setIsAppliedChanges(final Boolean isAppliedChanges) {
		this.isAppliedChanges = isAppliedChanges;
	}
	public Boolean getIsLogged() {
		return isLogged;
	}
	public void setIsLogged(final Boolean isLogged) {
		this.isLogged = isLogged;
	}
	public Boolean getIsIdentifier() {
		return isIdentifier;
	}
	public void setIsIdentifier(final Boolean isIdentifier) {
		this.isIdentifier = isIdentifier;
	}
	public Boolean getAreFutureDatesAllowed() {
		return areFutureDatesAllowed;
	}
	public void setAreFutureDatesAllowed(final Boolean areFutureDatesAllowed) {
		this.areFutureDatesAllowed = areFutureDatesAllowed;
	}
	public Boolean getIsDefaultToToday() {
		return isDefaultToToday;
	}
	public void setIsDefaultToToday(final Boolean isDefaultToToday) {
		this.isDefaultToToday = isDefaultToToday;
	}
	public Boolean getIsUsedForEscan() {
		return isUsedForEscan;
	}
	public void setIsUsedForEscan(final Boolean isUsedForEscan) {
		this.isUsedForEscan = isUsedForEscan;
	}
	public Boolean getIsUnique() {
		return isUnique;
	}
	public void setIsUnique(final Boolean isUnique) {
		this.isUnique = isUnique;
	}
	public Boolean getIsSearchable() {
		return isSearchable;
	}
	public void setIsSearchable(final Boolean isSearchable) {
		this.isSearchable = isSearchable;
	}
	public Boolean getIsSystemField() {
		return isSystemField;
	}
	public void setIsSystemField(final Boolean isSystemField) {
		this.isSystemField = isSystemField;
	}
	public Boolean getIsPrimaryKey() {
		return isPrimaryKey;
	}
	public void setIsPrimaryKey(final Boolean isPrimaryKey) {
		this.isPrimaryKey = isPrimaryKey;
	}
	public Boolean getIsValidationRequired() {
		return isValidationRequired;
	}
	public void setIsValidationRequired(final Boolean isValidationRequired) {
		this.isValidationRequired = isValidationRequired;
	}
	public Integer getIndexType() {
		return indexType;
	}
	public void setIndexType(final Integer indexType) {
		this.indexType = indexType;
	}
	public Integer getDataSize() {
		return dataSize;
	}
	public void setDataSize(final Integer dataSize) {
		this.dataSize = dataSize;
	}
	public Long getDataElementId() {
		return dataElementId;
	}
	public void setDataElementId(final Long dataElementId) {
		this.dataElementId = dataElementId;
	}
	public Long getPluginRegistrationId() {
		return pluginRegistrationId;
	}
	public void setPluginRegistrationId(final Long pluginRegistrationId) {
		this.pluginRegistrationId = pluginRegistrationId;
	}
	public String getElementName() {
		return elementName;
	}
	public void setElementName(final String elementName) {
		this.elementName = elementName;
	}
	public String getRequired() {
		return required;
	}
	public void setRequired(final String required) {
		this.required = required;
	}
	public String getDefaultValue() {
		return defaultValue;
	}
	public void setDefaultValue(final String defaultValue) {
		this.defaultValue = defaultValue;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(final String description) {
		this.description = description;
	}
	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}
	public String getmTableName() {
		return mTableName;
	}
	public void setmTableName(final String mTableName) {
		this.mTableName = mTableName;
	}
	public String getColumnName() {
		return columnName;
	}
	public void setColumnName(final String columnName) {
		this.columnName = columnName;
	}
	public String getBusinessKey() {
		return businessKey;
	}
	public void setBusinessKey(final String businessKey) {
		this.businessKey = businessKey;
	}
	public AeaEtkDataObject getEtkDataObject() {
		return etkDataObject;
	}
	public void setEtkDataObject(final AeaEtkDataObject etkDataObject) {
		this.etkDataObject = etkDataObject;
	}
	public AeaEtkLookupDefinition getEtkLookupDefinition() {
		return etkLookupDefinition;
	}
	public void setEtkLookupDefinition(final AeaEtkLookupDefinition etkLookupDefinition) {
		this.etkLookupDefinition = etkLookupDefinition;
	}
	public DataElementType getDataType() {
		return dataType;
	}
	public void setDataType(final DataElementType dataType) {
		this.dataType = dataType;
	}
}
