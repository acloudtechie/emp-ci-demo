/**
 *
 * Validates that the Code value for any new entries is unique.
 *
 * administrator 09/28/2016
 **/

package com.mptraining.refapp.rdo.roe;

import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.DataEventType;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.ReferenceObjectEventContext;
import com.entellitrak.configuration.DataObject;
import com.entellitrak.dynamic.DataObjectInstance;
import com.entellitrak.dynamic.DataObjectProperties;
import com.entellitrak.tracking.ReferenceObjectEventHandler;

public class ComplaintTypeValidator implements ReferenceObjectEventHandler {

    @Override
	public void execute(ReferenceObjectEventContext etk) throws ApplicationException {

        try {
    		ensureUniqueCode(etk);
		} catch (IncorrectResultSizeDataAccessException e) {
			e.printStackTrace();
			throw new ApplicationException(e);
		}

    }

    public static void ensureUniqueCode(final ReferenceObjectEventContext etk) throws IncorrectResultSizeDataAccessException {
        final DataObjectInstance newObject = etk.getNewObject();
        final DataObjectProperties properties = newObject.properties();
        final DataEventType dataEventType = etk.getDataEventType();
        final DataObject configuration = newObject.configuration();

        final String tableName = configuration.getTableName();

        if(DataEventType.CREATE == dataEventType
                || DataEventType.UPDATE == dataEventType){

            final Map<String, Object> duplicateInfo = etk.createSQL(String.format("SELECT C_CODE, COUNT(*) COUNT FROM %s WHERE c_code = (SELECT c_code FROM %s WHERE id = :trackingId) GROUP BY c_code",
                    tableName, tableName))
                    .setParameter("trackingId", properties.getId())
                    .fetchMap();
            final long matchingCodes = ((Number) duplicateInfo.get("COUNT")).longValue();

            if(1 < matchingCodes){
            	etk.getResult().cancelTransaction();
                etk.getResult().addMessage(String.format("Could not save the record because there is already a %s with Code \"%s\"",
                        configuration.getLabel(),
                        duplicateInfo.get("C_CODE")));
            }
        }
    }
}
