package net.micropact.aea.du.page.cleanRdoBaseObjectFlag;

import com.entellitrak.ApplicationException;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.DataObjectType;
import net.micropact.aea.utility.Utility;

/**
 * This serves as the Controller Code for a Page which updates the base_object field of data objects to the correct
 * value. This exists because old versions of the RDO creation utility created RDOs with base_object = 0 instead of 1.
 *
 * @author zachary.miller
 */
public class CleanRdoBaseObjectFlagController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();

            etk.createSQL("UPDATE etk_data_object SET base_object = 1 WHERE tracking_config_id = :trackingConfigId AND object_type = :objectType AND parent_object_id IS NULL")
            .setParameter("trackingConfigId", Utility.getTrackingConfigIdNext(etk))
            .setParameter("objectType", DataObjectType.REFERENCE.getEntellitrakNumber())
            .execute();

            return response;

        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }
}
