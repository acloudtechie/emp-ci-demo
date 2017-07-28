package net.micropact.aea.tt.page.getFormControlData;

import com.entellitrak.ApplicationException;
import com.entellitrak.DataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.Utility;

/**
 * This page controller returns information regarding the data elements on a particular form for use in
 * generating tooltips.
 *
 * @author zmiller
 */
public class GetFormControlDataController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();
            final String dataFormKey = etk.getParameters().getSingle("dataFormKey");
            response.setContentType(ContentType.JSON);
            response.put("out", etk.createSQL(Utility.isSqlServer(etk) ? "SELECT CASE WHEN de.data_element_id IS NOT NULL THEN do.object_name +'_' ELSE '' END + formControl.name NAME, formControl.tooltip_text TOOLTIPTEXT, de.description DESCRIPTION FROM etk_data_form dataForm JOIN etk_data_object DO ON do.data_object_id = dataForm.data_object_id JOIN etk_form_control formControl ON formControl.data_form_id = dataForm.data_form_id LEFT JOIN etk_form_ctl_element_binding controlElement ON controlElement.form_control_id = formControl.form_control_id LEFT JOIN etk_data_element de ON de.data_element_id = controlElement.data_element_id WHERE do.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive) AND dataForm.business_key = :dataFormBusinessKey"
                    : "SELECT CASE WHEN de.data_element_id IS NOT NULL THEN do.object_name || '_' ELSE '' END || formControl.name NAME, formControl.tooltip_text TOOLTIPTEXT, de.description DESCRIPTION FROM etk_data_form dataForm JOIN etk_data_object DO ON do.data_object_id = dataForm.data_object_id JOIN etk_form_control formControl ON formControl.data_form_id = dataForm.data_form_id LEFT JOIN etk_form_ctl_element_binding controlElement ON controlElement.form_control_id = formControl.form_control_id LEFT JOIN etk_data_element de ON de.data_element_id = controlElement.data_element_id WHERE do.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive ) AND dataForm.business_key = :dataFormBusinessKey")
                    .setParameter("dataFormBusinessKey", dataFormKey)
                    .fetchJSON());
            return response;
        } catch (final DataAccessException e) {
            throw new ApplicationException(e);
        }
    }
}
