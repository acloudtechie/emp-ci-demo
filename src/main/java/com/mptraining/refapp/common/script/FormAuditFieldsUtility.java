/**
 *
 * Utility class for easily filling in the usual created / updated fields on data forms.
 *
 * administrator 09/27/2016
 **/

package com.mptraining.refapp.common.script;

import java.util.Date;

import com.entellitrak.ApplicationException;
import com.entellitrak.DataEventType;
import com.entellitrak.DataObjectEventContext;
import com.entellitrak.InputValidationException;
import com.entellitrak.dynamic.DataObjectInstance;

public class FormAuditFieldsUtility {

	/**
	 * Updates Created Date/User or Updated Date/User during
	 * Create or Update events ONLY
	 * @param etk
	 * @throws ApplicationException
	 */
	public static void updateObjectAudit(DataObjectEventContext etk, DataObjectInstance dataObject) throws ApplicationException{
		try {
			//Class<? extends DataObjectInstance> objectClass = DynamicObjectConfigurationUtils.getDynamicClass(etk, etk.getNewObject().configuration().getBusinessKey());
			//DataObjectInstance dataObject = etk.getNewObject();//etk.getDynamicObjectService().get(objectClass, etk.getNewObject().properties().getId());
			if(DataEventType.CREATE.equals(etk.getDataEventType())){
				dataObject.set("createdOn", new Date());
				dataObject.set("createdBy", etk.getCurrentUser().getId().intValue());
				etk.getDynamicObjectService().createSaveOperation(dataObject).setExecuteEvents(false).save();
			}
			else if(DataEventType.UPDATE.equals(etk.getDataEventType())){
				dataObject.set("updatedOn", new Date());
				dataObject.set("updatedBy", etk.getCurrentUser().getId().intValue());
				etk.getDynamicObjectService().createSaveOperation(dataObject).setExecuteEvents(false).save();
			}

		} catch (InputValidationException e) {
			e.printStackTrace();
			throw new ApplicationException(e);
		}


	}
}
