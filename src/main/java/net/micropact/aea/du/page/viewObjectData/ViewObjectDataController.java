package net.micropact.aea.du.page.viewObjectData;

import com.entellitrak.ApplicationException;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.DataObjectType;
import net.micropact.aea.utility.Utility;

/**
 * This class is the controller code for a page which can be used to view the data for a BTO and all of its descendants.
 *
 * @author zachary.miller
 */
public class ViewObjectDataController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();
            response.setContentType(ContentType.HTML);

            response.put("dataObjects", etk.createSQL("SELECT LABEL, BUSINESS_KEY FROM etk_data_object WHERE tracking_config_id = :trackingConfigId AND object_type = :objectType AND base_object = 1 ORDER BY LABEL, BUSINESS_KEY")
                    .setParameter("trackingConfigId", Utility.getTrackingConfigIdCurrent(etk))
                    .setParameter("objectType", DataObjectType.TRACKING.getEntellitrakNumber())
                    .fetchJSON());

            return response;
        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }
}
