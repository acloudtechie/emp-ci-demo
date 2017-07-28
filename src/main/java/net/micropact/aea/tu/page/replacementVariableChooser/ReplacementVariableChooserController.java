package net.micropact.aea.tu.page.replacementVariableChooser;

import java.util.Arrays;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.DataElementType;

/**
 * This contains controller code for a GUI which allows a user to select replacement variables for use in
 * different HTML templates.
 * @author zmiller
 * @see net.entellitrak.aea.tu.Templater
 * @see net.entellitrak.aea.tu.replacers.DataElementReplacer
 * @see net.entellitrak.aea.tu.replacers.SectionReplacer
 * @see net.entellitrak.aea.tu.replacers.SqlReplacer
 */
public class ReplacementVariableChooserController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {
            final TextResponse response = etk.createTextResponse();

            response.put("replacementVariables", etk.createSQL("SELECT C_NAME FROM t_tu_replacement_variable ORDER BY c_name").fetchJSON());
            response.put("replacementSections", etk.createSQL("SELECT C_NAME FROM t_tu_replacement_section ORDER BY c_name").fetchJSON());

            response.put("dataObjects", etk.createSQL("SELECT do.DATA_OBJECT_ID, do.LABEL, do.OBJECT_NAME, do.NAME FROM etk_data_object do WHERE do.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config) ORDER BY NAME, LABEL, OBJECT_NAME, DATA_OBJECT_ID").fetchJSON());
            response.put("dataElements", etk.createSQL("SELECT de.DATA_OBJECT_ID, de.NAME, de.ELEMENT_NAME FROM etk_data_object DO JOIN etk_data_element de ON de.data_object_id = do.data_object_id WHERE do.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config ) AND (de.bound_to_lookup IS NULL OR de.bound_to_lookup = 0) AND (de.data_type NOT IN(:excludedDataTypes)) ORDER BY NAME, ELEMENT_NAME, DATA_OBJECT_ID")
                    .setParameter("excludedDataTypes", Arrays.asList(DataElementType.NONE.getEntellitrakNumber()))
                    .fetchJSON());

            return response;
    }
}
