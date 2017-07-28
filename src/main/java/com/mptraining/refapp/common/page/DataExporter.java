/**
 *
 * Exports Workflow Rules Engine config, System Preferences, and RDO data to files to be saved in the system and used when importing to a new system.
 *
 * administrator 05/03/2017
 **/

package com.mptraining.refapp.common.page;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.configuration.LanguageType;
import com.entellitrak.configuration.Script;
import com.entellitrak.dynamic.SystemInformation;
import com.entellitrak.file.File;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;
import com.entellitrak.platform.DatabasePlatform;
import com.mptraining.refapp.common.dao.EtkTablesDao;
import com.mptraining.refapp.rdo.dao.RdoDao;

import net.entellitrak.aea.dbUtils.RdoDataExporter;
import net.entellitrak.aea.du.DuServiceFactory;
import net.entellitrak.aea.rf.RfServiceFactory;

public class DataExporter implements PageController {

    public Response execute(PageExecutionContext etk) throws ApplicationException {

    	TextResponse response = etk.createTextResponse();
    	
    	//Set which System Preferences should be exported
    	Set<String> preferencesToExport = new HashSet<String>();
    	preferencesToExport.add("autoRememberSearchCriteria");
    	preferencesToExport.add("caseSensitiveUsernameAuthentication");
    	preferencesToExport.add("customLoginScreenAgree");
    	preferencesToExport.add("customLoginScreenHeader");
    	preferencesToExport.add("customLoginScreenText");
    	preferencesToExport.add("enableAdvancedSearch");
    	preferencesToExport.add("enableAdvancedSearchLookupContext");
    	preferencesToExport.add("enableDocumentManagement");
    	preferencesToExport.add("enableEndpoints");
    	preferencesToExport.add("enableExportToPdf");
    	preferencesToExport.add("enableFormControlTooltip");
    	preferencesToExport.add("enableFormPDFPrinting");
    	preferencesToExport.add("enableHtmlEscaping");
    	preferencesToExport.add("enableMobileInbox");
    	preferencesToExport.add("enableOAuth2Authentication");
    	preferencesToExport.add("enablePasswordResetFeature");
    	preferencesToExport.add("enablePrintPermissions");
    	preferencesToExport.add("enablePrinterFriendlyFormatAndPrint");
    	preferencesToExport.add("enablePublicPages");
    	preferencesToExport.add("enableQuickSearch");
    	preferencesToExport.add("enableRedirectOnSessionTimeout");
    	preferencesToExport.add("enableSingleResultLookupContext");
    	preferencesToExport.add("enableViewFilters");
    	preferencesToExport.add("enforcePasswordHistory");
    	preferencesToExport.add("passwordsExpire");
    	preferencesToExport.add("searchQueryTimeout");
    	preferencesToExport.add("useCustomLoginScreen");
		
    	try(InputStream workflowExportStream = RfServiceFactory.getRfMigrationService(etk).exportToStream();
    		InputStream preferenceExportStream = DuServiceFactory.getSystemPreferenceMigrationService(etk).exportToStream(preferencesToExport);	) {
			
			//Save the Workflow Rules Engine configuration into the System Information RDO
			File workflowFile = etk.getFileService().create(workflowExportStream, "rf_export.xml", "xml", "text/xml");
			Long workflowSystemInfoId = RdoDao.getIdFromCode(etk, "T_SYSTEM_INFORMATION", "wre.configExport").longValue();
			SystemInformation workflowInfo = etk.getDynamicObjectService().get(SystemInformation.class, workflowSystemInfoId);
			String fileElementKey = "object.systemInformation.element.file";
			etk.getFileService().attachFile(workflowFile, workflowSystemInfoId, workflowInfo.configuration().getBusinessKey(), fileElementKey);
			
			//Save the System Preferences configuration into the System Information RDO
			File preferenceFile = etk.getFileService().create(preferenceExportStream, "systemPreferenceExport.xml", "xml", "text/xml");
			Long preferenceSystemInfoId = RdoDao.getIdFromCode(etk, "T_SYSTEM_INFORMATION", "sys.preferences").longValue();
			etk.getFileService().attachFile(preferenceFile, preferenceSystemInfoId, workflowInfo.configuration().getBusinessKey(), fileElementKey);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new ApplicationException(e);
		}
    	
    	//Generate RDO data export
    	Collection<Script> sqlScripts = etk.getWorkspaceService().getScriptsByLanguageType(etk.getWorkspaceService().getSystemWorkspace(), LanguageType.SQL);
    	for (Script script : sqlScripts) {
			if("RdoData".equals(script.getName())){
				String newCode = RdoDataExporter.exportToSqlOneStageUsingLastSavedConfig(etk, DatabasePlatform.ORACLE, false, false);
				EtkTablesDao.updateScriptCode(etk, script.getId(), newCode);
				break;
			}
		}
    	
    	response.put("out", "Data successfully exported for deployment.");
		return response;

    }

}
