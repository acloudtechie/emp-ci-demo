package net.micropact.aea.du.page.lookupDefinitionUsageAjax;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * This page is used by the Lookup Definitions Usage page. It returns data indicating where various lookups
 * are being used.
 * @author zmiller
 */
public class LookupDefinitionUsageAjaxController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {
        try {

            final TextResponse response = etk.createTextResponse();

            response.setContentType(ContentType.JSON);

            response.put("out", JsonUtilities.encode(Utility.arrayToMap(String.class, Object.class, new Object[][]{
                {"lookupDefinitions", etk.createSQL("SELECT lookupDefinition.LOOKUP_DEFINITION_ID, lookupDefinition.NAME, lookupDefinition.ENABLE_CACHING FROM etk_lookup_definition lookupDefinition WHERE lookupDefinition.tracking_config_id = (SELECT tracking_config_id FROM etk_tracking_config WHERE config_version = (SELECT MAX(config_version) FROM etk_tracking_config ) ) ORDER BY lookupDefinition.name, lookupDefinition.lookup_definition_id")
                        .fetchList()},
                {"dataElements", etk.createSQL("SELECT dataObject.label DATA_OBJECT_LABEL, dataObject.object_name DATA_OBJECT_NAME, dataElement.NAME DATA_ELEMENT_NAME, dataElement.element_name DATA_ELEMENT_ELEMENT_NAME, dataElement.LOOKUP_DEFINITION_ID, dataElement.DATA_ELEMENT_ID, dataObject.DATA_OBJECT_ID FROM etk_data_object dataObject JOIN etk_data_element dataElement ON dataElement.data_object_id = dataObject.data_object_id JOIN etk_lookup_definition lookupDefinition ON lookupDefinition.lookup_definition_id = dataElement.lookup_definition_id WHERE dataObject.tracking_config_id = (SELECT tracking_config_id FROM etk_tracking_config WHERE config_version = ( SELECT MAX(config_version) FROM etk_tracking_config ) ) ORDER BY DATA_OBJECT_LABEL, DATA_ELEMENT_NAME, dataElement.data_element_id")
                        .fetchList()},
                {"formControls", etk.createSQL("SELECT lookupDefinition.LOOKUP_DEFINITION_ID, dataObject.DATA_OBJECT_ID, formControl.name FORM_CONTROL_NAME, dataForm.name DATA_FORM_NAME, dataObject.label DATA_OBJECT_LABEL, dataObject.object_name DATA_OBJECT_NAME, dataForm.DATA_FORM_ID FROM etk_lookup_definition lookupDefinition JOIN etk_form_ctl_lookup_binding lookupBinding ON lookupBinding.lookup_definition_id = lookupDefinition.lookup_definition_id JOIN etk_form_control formControl ON formControl.form_control_id = lookupBinding.form_control_id JOIN etk_data_form dataForm ON dataForm.data_form_id = formControl.data_form_id JOIN etk_data_object dataObject ON dataObject.data_object_id = dataForm.data_object_id WHERE lookupDefinition.tracking_config_id = (SELECT tracking_config_id FROM etk_tracking_config WHERE config_version = ( SELECT MAX(config_version) FROM etk_tracking_config ) ) ORDER BY data_object_label, data_form_name, form_control_name, lookup_definition_id")
                        .fetchList()}
            })));

            return response;
        } catch (final Exception e) {
            throw new ApplicationException(e);
        }
    }
}
