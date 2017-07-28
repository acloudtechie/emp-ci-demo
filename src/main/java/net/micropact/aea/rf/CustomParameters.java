package net.micropact.aea.rf;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.entellitrak.ExecutionContext;

import net.entellitrak.aea.rf.ICustomParameters;

/**
 * This class contains the private implementation of the public {@link ICustomParameters} interface.
 * @author zmiller
 */
public class CustomParameters implements ICustomParameters {

    private final Map<String, String> singles;
    private final Map<String, List<String>> multiples;

    /**
     * {@link CustomParameters} constructor.
     *
     * @param etk entellitrak execution context
     * @param workflowEffectId The tracking id of the RF Workflow Effect which these parameters are being generated for
     * */
    public CustomParameters(final ExecutionContext etk, final long workflowEffectId) {

        singles = new HashMap<>();
        multiples = new HashMap<>();

        /* Here we set up the parameters that are specific to this RfScript */
        final List<Map<String, Object>> customParameters = etk.createSQL("SELECT scriptParameter.c_code SCRIPTPARAMETERCODE, scriptParameter.c_allow_multiple ALLOWMULTIPLE, scriptParameterValue.c_value VALUE FROM t_rf_workflow_effect workflowEffect JOIN t_rf_script_parameter scriptParameter ON scriptParameter.id_parent = workflowEffect.c_script LEFT JOIN t_rf_script_parameter_value scriptParameterValue ON scriptParameterValue.id_parent = workflowEffect.id AND scriptParameterValue.c_script_parameter = scriptParameter.id WHERE workflowEffect.id = :workflowEffectId ORDER BY scriptParameterValue.id")
                .setParameter("workflowEffectId", workflowEffectId)
                .fetchList(); /* SCRIPTPARAMETERCODE, ALLOWMULTIPLE, VALUE*/

        /* Initialize all the customParameters to default values */
        customParameters.forEach(parameter -> {
            if("1".equals(parameter.get("ALLOWMULTIPLE")+"")){
                multiples.put((String) parameter.get("SCRIPTPARAMETERCODE"), new LinkedList<String>());
            }else{
                singles.put((String) parameter.get("SCRIPTPARAMETERCODE"), null);
            }
        });

        /* Set the values of the customParameters */
        customParameters.forEach(parameter -> {
            if(null != parameter.get("VALUE")){
                if("1".equals(parameter.get("ALLOWMULTIPLE")+"")){
                    multiples.get(parameter.get("SCRIPTPARAMETERCODE")).add((String) parameter.get("VALUE"));
                }else{
                    singles.put((String) parameter.get("SCRIPTPARAMETERCODE"), (String) parameter.get("VALUE"));
                }
            }
        });
    }

    @Override
    public String getSingle(final String parameterCode){
        return singles.get(parameterCode);
    }

    @Override
    public List<String> getMultiple(final String parameterCode){
        return multiples.get(parameterCode);
    }
}
