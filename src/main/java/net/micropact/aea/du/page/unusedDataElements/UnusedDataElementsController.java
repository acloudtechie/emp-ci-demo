package net.micropact.aea.du.page.unusedDataElements;

import com.entellitrak.ApplicationException;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.Utility;

/**
 * Controller code for a page which displays potentially unused data elements.
 *
 * @author zmiller
 */
public class UnusedDataElementsController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {

            final TextResponse response = etk.createTextResponse();

            response.put("unusedDataElements",
                    etk.createSQL("SELECT dataObject.DATA_OBJECT_ID, dataObject.OBJECT_NAME, dataElement.DATA_ELEMENT_ID, dataElement.name DATA_ELEMENT_NAME, CASE WHEN EXISTS(SELECT * FROM etk_data_view_element dataViewElement WHERE dataViewElement.data_element_id = dataElement.data_element_id) THEN 1 ELSE 0 END ON_VIEW FROM etk_data_object dataObject JOIN etk_data_element dataElement ON dataElement.data_object_id = dataObject.data_object_id WHERE dataObject.tracking_config_id = :trackingConfigId AND dataElement.system_field = 0 AND NOT EXISTS( SELECT * FROM etk_form_ctl_element_binding formCtlElementBinding WHERE formCtlElementBinding.data_element_id = dataElement.data_element_id ) ORDER BY OBJECT_NAME, DATA_ELEMENT_NAME, DATA_ELEMENT_ID")
                    .setParameter("trackingConfigId", Utility.getTrackingConfigIdNext(etk))
                    .fetchJSON());

            return response;

        }catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }
}
