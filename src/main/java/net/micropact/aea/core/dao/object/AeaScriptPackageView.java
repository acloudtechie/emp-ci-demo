package net.micropact.aea.core.dao.object;

import net.micropact.aea.utility.PackageType;
import net.micropact.aea.utility.ScriptObjectHandlerType;
import net.micropact.aea.utility.ScriptObjectLanguageType;

public class AeaScriptPackageView {

	private ScriptObjectLanguageType scriptObjectLanguageType;
	private ScriptObjectHandlerType scriptObjectHandlerType;
	private PackageType packageType;
	private String scriptName;
	private String packagePath;
	private String fullyQualifiedScriptName;
	private String scriptBusinessKey;

	private Number scriptId;
	private Number workspaceId;
	private Number trackingConfigId;
	private Number packageNodeId;

	public String getScriptName() {
		return scriptName;
	}
	public void setScriptName(String scriptName) {
		this.scriptName = scriptName;
	}
	public ScriptObjectLanguageType getScriptObjectLanguageType() {
		return scriptObjectLanguageType;
	}
	public void setScriptObjectLanguageType(ScriptObjectLanguageType scriptObjectLanguageType) {
		this.scriptObjectLanguageType = scriptObjectLanguageType;
	}
	public String getScriptBusinessKey() {
		return scriptBusinessKey;
	}
	public void setScriptBusinessKey(String scriptBusinessKey) {
		this.scriptBusinessKey = scriptBusinessKey;
	}
	public Number getWorkspaceId() {
		return workspaceId;
	}
	public void setWorkspaceId(Number workspaceId) {
		this.workspaceId = workspaceId;
	}
	public Number getTrackingConfigId() {
		return trackingConfigId;
	}
	public void setTrackingConfigId(Number trackingConfigId) {
		this.trackingConfigId = trackingConfigId;
	}
	public String getPackagePath() {
		return packagePath;
	}
	public void setPackagePath(String packagePath) {
		this.packagePath = packagePath;
	}
	public Number getPackageNodeId() {
		return packageNodeId;
	}
	public void setPackageNodeId(Number packageNodeId) {
		this.packageNodeId = packageNodeId;
	}
	public PackageType getPackageType() {
		return packageType;
	}
	public void setPackageType(PackageType packageType) {
		this.packageType = packageType;
	}
	public String getFullyQualifiedScriptName() {
		return fullyQualifiedScriptName;
	}
	public void setFullyQualifiedScriptName(String fullyQualifiedScriptName) {
		this.fullyQualifiedScriptName = fullyQualifiedScriptName;
	}
	public ScriptObjectHandlerType getScriptObjectHandlerType() {
		return scriptObjectHandlerType;
	}
	public void setScriptObjectHandlerType(ScriptObjectHandlerType scriptObjectHandlerType) {
		this.scriptObjectHandlerType = scriptObjectHandlerType;
	}
	public Number getScriptId() {
		return scriptId;
	}
	public void setScriptId(Number scriptId) {
		this.scriptId = scriptId;
	}
}
