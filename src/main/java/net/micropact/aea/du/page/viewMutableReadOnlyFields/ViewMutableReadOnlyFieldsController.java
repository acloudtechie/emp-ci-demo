package net.micropact.aea.du.page.viewMutableReadOnlyFields;

import com.entellitrak.ApplicationException;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.Utility;

/**
 * This class serves as the controller code for a class which displays all fields which are both mutable and read-only.
 *
 * @author zachary.miller
 */
public class ViewMutableReadOnlyFieldsController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
        final TextResponse response = etk.createTextResponse();

            response.put("fields", etk.createSQL("SELECT dataObject.name OBJECT_NAME, dataObject.data_object_id DATA_OBJECT_ID, dataForm.name FORM_NAME, dataForm.data_form_id DATA_FORM_ID, formControl.name FORM_CONTROL_NAME FROM etk_data_object dataObject JOIN etk_data_form dataForm ON dataForm.data_object_id = dataObject.data_object_id JOIN etk_form_control formControl ON formControl.data_form_id = dataForm.data_form_id WHERE dataObject.tracking_config_id = :trackingConfigId AND formControl.mutable_read_only = 1 ORDER BY dataObject.name, dataObject.data_object_id, dataForm.name, dataForm.data_form_id, formControl.display_order, formControl.name")
                    .setParameter("trackingConfigId", Utility.getTrackingConfigIdNext(etk))
                    .fetchJSON());

        return response;

        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }
}
