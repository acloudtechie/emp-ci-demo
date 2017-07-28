package net.micropact.aea.utility.lookup;

import net.micropact.aea.utility.DataElementType;
import net.micropact.aea.utility.LookupSourceType;
import net.micropact.aea.utility.SystemObjectDisplayFormat;
import net.micropact.aea.utility.SystemObjectType;

public class AeaEtkLookupDefinition {

	String lookupSql;
	Boolean ascendingOrder;
	Boolean enableCaching;
	AeaEtkDataObject etkDataObject;

	AeaEtkDataElement valueElement;
	AeaEtkDataElement displayElement;
	AeaEtkDataElement startDateElement;
	AeaEtkDataElement endDateElement;
	AeaEtkDataElement orderByElement;

	LookupSourceType lookupType;
	DataElementType valueReturnType;
	SystemObjectType systemObjectType;
	SystemObjectDisplayFormat systemObjectDisplayFormat;

	Long lookupDefinitonId;
	Long pluginRegistrationId;
	Long sqlScriptObjectId;
	Long trackingConfigId;

	String name;
	String description;
	String business_key;

	public String getLookupSql() {
		return lookupSql;
	}
	public void setLookupSql(final String lookupSql) {
		this.lookupSql = lookupSql;
	}
	public Boolean getAscendingOrder() {
		return ascendingOrder;
	}
	public void setAscendingOrder(final Boolean ascendingOrder) {
		this.ascendingOrder = ascendingOrder;
	}
	public AeaEtkDataObject getEtkDataObject() {
		return etkDataObject;
	}
	public void setEtkDataObject(final AeaEtkDataObject etkDataObject) {
		this.etkDataObject = etkDataObject;
	}
	public AeaEtkDataElement getValueElement() {
		return valueElement;
	}
	public void setValueElement(final AeaEtkDataElement valueElement) {
		this.valueElement = valueElement;
	}
	public AeaEtkDataElement getDisplayElement() {
		return displayElement;
	}
	public void setDisplayElement(final AeaEtkDataElement displayElement) {
		this.displayElement = displayElement;
	}
	public AeaEtkDataElement getStartDateElement() {
		return startDateElement;
	}
	public void setStartDateElement(final AeaEtkDataElement startDateElement) {
		this.startDateElement = startDateElement;
	}
	public AeaEtkDataElement getEndDateElement() {
		return endDateElement;
	}
	public void setEndDateElement(final AeaEtkDataElement endDateElement) {
		this.endDateElement = endDateElement;
	}
	public AeaEtkDataElement getOrderByElement() {
		return orderByElement;
	}
	public void setOrderByElement(final AeaEtkDataElement orderByElement) {
		this.orderByElement = orderByElement;
	}
	public LookupSourceType getLookupType() {
		return lookupType;
	}
	public void setLookupType(final LookupSourceType lookupType) {
		this.lookupType = lookupType;
	}
	public DataElementType getValueReturnType() {
		return valueReturnType;
	}
	public void setValueReturnType(final DataElementType valueReturnType) {
		this.valueReturnType = valueReturnType;
	}
	public Long getLookupDefinitonId() {
		return lookupDefinitonId;
	}
	public void setLookupDefinitonId(final Long lookupDefinitonId) {
		this.lookupDefinitonId = lookupDefinitonId;
	}
	public Long getPluginRegistrationId() {
		return pluginRegistrationId;
	}
	public void setPluginRegistrationId(final Long pluginRegistrationId) {
		this.pluginRegistrationId = pluginRegistrationId;
	}
	public Long getSqlScriptObjectId() {
		return sqlScriptObjectId;
	}
	public void setSqlScriptObjectId(final Long sqlScriptObjectId) {
		this.sqlScriptObjectId = sqlScriptObjectId;
	}
	public Long getTrackingConfigId() {
		return trackingConfigId;
	}
	public void setTrackingConfigId(final Long trackingConfigId) {
		this.trackingConfigId = trackingConfigId;
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
	public String getBusiness_key() {
		return business_key;
	}
	public void setBusiness_key(final String business_key) {
		this.business_key = business_key;
	}
	public Boolean getEnableCaching() {
		return enableCaching;
	}
	public void setEnableCaching(Boolean enableCaching) {
		this.enableCaching = enableCaching;
	}
	public SystemObjectType getSystemObjectType() {
		return systemObjectType;
	}
	public void setSystemObjectType(SystemObjectType systemObjectType) {
		this.systemObjectType = systemObjectType;
	}
	public SystemObjectDisplayFormat getSystemObjectDisplayFormat() {
		return systemObjectDisplayFormat;
	}
	public void setSystemObjectDisplayFormat(
			SystemObjectDisplayFormat systemObjectDisplayFormat) {
		this.systemObjectDisplayFormat = systemObjectDisplayFormat;
	}

}
