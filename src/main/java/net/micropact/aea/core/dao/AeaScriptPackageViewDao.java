package net.micropact.aea.core.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.entellitrak.ExecutionContext;

import net.micropact.aea.core.dao.object.AeaScriptPackageView;
import net.micropact.aea.utility.PackageType;
import net.micropact.aea.utility.ScriptObjectHandlerType;
import net.micropact.aea.utility.ScriptObjectLanguageType;

public class AeaScriptPackageViewDao {

	public static List<AeaScriptPackageView> getAllAeaScriptPackageViewSysOnly (ExecutionContext etk) {
		List<Map<String, Object>> scriptMapList =
				etk.createSQL("select * from AEA_SCRIPT_PKG_VIEW_SYS_ONLY").fetchList();

		List<AeaScriptPackageView> packageViewList = new ArrayList<AeaScriptPackageView>();

		for (Map<String, Object> scriptMap : scriptMapList) {
			packageViewList.add(initPackageViewFromMap(scriptMap));
		}

		return packageViewList;
	}


	private static AeaScriptPackageView initPackageViewFromMap(final Map<String, Object> aPackageViewMap) {

		AeaScriptPackageView spv = new AeaScriptPackageView();

		spv.setFullyQualifiedScriptName((String) aPackageViewMap.get("FULLY_QUALIFIED_SCRIPT_NAME"));
		spv.setPackageNodeId((Number) aPackageViewMap.get("PACKAGE_NODE_ID"));
		spv.setPackagePath((String) aPackageViewMap.get("PACKAGE_PATH"));
		spv.setPackageType(PackageType.getPackageType((Number) aPackageViewMap.get("PACKAGE_TYPE")));
		spv.setScriptBusinessKey((String) aPackageViewMap.get("SCRIPT_BUSINESS_KEY"));
		spv.setScriptId((Number) aPackageViewMap.get("SCRIPT_ID"));
		spv.setScriptName((String) aPackageViewMap.get("SCRIPT_NAME"));
		spv.setScriptObjectHandlerType(ScriptObjectHandlerType.getById((Number) aPackageViewMap.get("SCRIPT_HANDLER_TYPE")));
		spv.setScriptObjectLanguageType(ScriptObjectLanguageType.getById((Number) aPackageViewMap.get("SCRIPT_LANGUAGE_TYPE")));
		spv.setTrackingConfigId((Number) aPackageViewMap.get("TRACKING_CONFIG_ID"));
		spv.setWorkspaceId((Number) aPackageViewMap.get("WORKSPACE_ID"));

		return spv;

	}
}
