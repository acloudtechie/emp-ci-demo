package net.micropact.aea.du.page.trackedDataObjectGraphAjax;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

/**
 * This page is used by the Tracked Data Object graph page. It purpose is to return meta-data about the data objects
 * in the system.
 * @author zmiller
 */
public class TrackedDataObjectGraphAjaxController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {

            final TextResponse response = etk.createTextResponse();

            response.setContentType(ContentType.JSON);

            response.put(
                    "out",
                    etk.createSQL("SELECT dataObject.DATA_OBJECT_ID, dataObject.PARENT_OBJECT_ID, dataObject.LABEL, dataObject.TABLE_NAME, dataObject.NAME, dataObject.OBJECT_NAME, dataObject.BUSINESS_KEY FROM etk_data_object dataObject WHERE dataObject.tracking_config_id = ( SELECT tracking_config_id FROM etk_tracking_config WHERE config_version = ( SELECT MAX(config_version) FROM etk_tracking_config ) ) AND dataObject.object_type = 1 ORDER BY dataObject.LIST_ORDER, dataObject.label")
                    .fetchJSON());

            return response;
    }
}
