/**
 *
 * Loads Reference Data and Workflow Rules Engine configuration after a new site is deployed.
 *
 * administrator 02/28/2017
 **/

package com.mptraining.refapp.common.script;

import java.io.InputStream;

import net.entellitrak.aea.core.CoreServiceFactory;
import net.entellitrak.aea.du.DuServiceFactory;
import net.entellitrak.aea.rf.RfServiceFactory;

import com.entellitrak.ApplicationException;
import com.entellitrak.dynamic.SystemInformation;
import com.entellitrak.system.DeploymentExecutionContext;
import com.entellitrak.system.DeploymentHandler;
import com.mptraining.refapp.rdo.dao.RdoDao;

public class RefAppDataLoader implements DeploymentHandler {

	@Override
	public void execute(DeploymentExecutionContext etk) throws ApplicationException {

		CoreServiceFactory.getDeploymentService(etk).runComponentSetup();
		
		// Load RDO data
		etk.executeSQLFromScriptObject("com.mptraining.refapp.rdo.RdoData");
		
		try {
			//Load Workflow Rules Engine configuration from the System Information RDO
			Long workflowSystemInfoId = RdoDao.getIdFromCode(etk, "T_SYSTEM_INFORMATION", "wre.configExport").longValue();
			SystemInformation workflowInfo = etk.getDynamicObjectService().get(SystemInformation.class, workflowSystemInfoId);
			InputStream workflowContentStream = etk.getFileService().get(workflowInfo.getFile()).getContent();
			RfServiceFactory.getRfMigrationService(etk).importFromStream(workflowContentStream);
			workflowContentStream.close();
			
			//Load System Preferences configuration from the System Information RDO
			Long preferenceSystemInfoId = RdoDao.getIdFromCode(etk, "T_SYSTEM_INFORMATION", "sys.preferences").longValue();
			SystemInformation preferenceInfo = etk.getDynamicObjectService().get(SystemInformation.class, preferenceSystemInfoId);
			InputStream preferenceContentStream = etk.getFileService().get(preferenceInfo.getFile()).getContent();
			DuServiceFactory.getSystemPreferenceMigrationService(etk).importFromStream(preferenceContentStream);
			preferenceContentStream.close();
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new ApplicationException(e);
		}

	}
}