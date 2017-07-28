package net.micropact.aea.rf.page.dynamicParametersAjax;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.rf.utility.dynamicParameters.DynamicParametersUseFactory;
import net.micropact.aea.rf.utility.dynamicParameters.DynamicParametersUseFactory.DynamicParameterUsage;
import net.micropact.aea.rf.utility.dynamicParameters.IDynamicParameterUseInfo;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * Controller code for a page which fetches the metadata for dynamic parameters.
 * This data includes for instance the RF Script Parameter information for for a particular RF Script.
 *
 * @author zmiller
 */
public class DynamicParametersAjaxController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        /* We take in a parameterDefinerParentId and workflowId and will return information about the parameters to be
         * used to build the UI. Unfortunately, we currently don't have a good way to create nested JSON, so this is
         * going to return the two lists separately and it will be the job of the user match them up with each other.
         *
         * NOTE: We now have a good way to do nested JSON! so we can change this if we want to!
         * */

        final TextResponse response = etk.createTextResponse();
        response.setContentType(ContentType.JSON);

        final String parameterDefinerParentIdParameter = etk.getParameters().getSingle("parameterDefinerParentId");
        final String rfWorkflowId = etk.getParameters().getSingle("rfWorkflowId");
        final String dynamicParameterType = etk.getParameters().getSingle("dynamicParameterType");

        final String parameterDefinerParentId =
                "".equals(parameterDefinerParentIdParameter) ? null : parameterDefinerParentIdParameter;

        final IDynamicParameterUseInfo dynamicParametersInfo =
                DynamicParametersUseFactory.loadDynamicParameterUseInfo(etk,
                        DynamicParameterUsage.valueOf(dynamicParameterType));

        final List<Map<String, Object>> parameters = etk.createSQL(
                String.format("SELECT parameter.id PARAMETERID, parameter.c_name PARAMETERNAME, parameterType.c_code PARAMETERTYPECODE, lookup.c_sql LOOKUPSQL, parameter.c_required REQUIRED, parameter.c_allow_multiple ALLOWMULTIPLE, parameter.c_description DESCRIPTION FROM %s parameter JOIN t_rf_parameter_type parameterType ON parameterType.id = parameter.c_type LEFT JOIN t_rf_lookup lookup ON lookup.id = parameter.c_lookup WHERE parameter.id_parent = :parameterDefinerParentId ORDER BY parameter.c_order, parameter.c_name",
                        dynamicParametersInfo.getParameterDefiningObject().getTableName()))
                .setParameter("parameterDefinerParentId", parameterDefinerParentId)
                .fetchList();
        /*PARAMETERID, PARAMETERNAME, PARAMETERTYPECODE, LOOKUPSQL, REQUIRED, ALLOWMULTIPLE, DESCRIPTION*/

        /* Handle the parameters of type lookup */
        /*VALUE DISPLAY*/
        final Stream<List<Map<String, Object>>> lookupParameters = parameters.stream()
                .filter(parameter -> "lookup".equals(parameter.get("PARAMETERTYPECODE")))
                .map(parameter ->
                    etk.createSQL((String) parameter.get("LOOKUPSQL"))
                    .setParameter("rfWorkflowId", rfWorkflowId)
                    .fetchList());

        response.put("out", JsonUtilities.encode(Utility.arrayToMap(String.class, Object.class, new Object[][]{
            {"parameters", parameters},
            {"lookupParameters", lookupParameters}
        })));

        return response;
    }
}
