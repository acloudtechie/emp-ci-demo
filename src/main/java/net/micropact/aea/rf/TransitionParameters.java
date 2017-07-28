package net.micropact.aea.rf;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.entellitrak.ExecutionContext;

import net.entellitrak.aea.rf.ITransitionParameters;

/**
 * This class contains the implementation of {@link ITransitionParameters}.
 *
 * @author zmiller
 */
public class TransitionParameters implements ITransitionParameters {

    private final Map<String, String> singles;
    private final Map<String, List<String>> multiples;

    /**
     * Creates a new Transition Parameters based on the transition's tracking id.
     *
     * @param etk entellitrak execution context
     * @param transitionId RF Transition Id
     */
    public TransitionParameters(final ExecutionContext etk, final long transitionId) {
        singles = new HashMap<>();
        multiples = new HashMap<>();

        /* Here we set up the parameters that are specific to this RF Transition */
        final List<Map<String, Object>> transitionParameters = etk.createSQL("SELECT workflowParameter.c_code PARAMETERCODE, workflowParameter.c_allow_multiple ALLOWMULTIPLE, transitionParameterValue.c_value VALUE FROM t_rf_transition transition JOIN t_rf_workflow_parameter workflowParameter ON workflowParameter.id_parent = transition.id_parent LEFT JOIN t_rf_transition_parameter_valu transitionParameterValue ON transitionParameterValue.id_parent = transition.id AND transitionParameterValue.c_workflow_parameter = workflowParameter.id WHERE transition.id = :transitionId ORDER BY transitionParameterValue.id ")
                .setParameter("transitionId", transitionId)
                .fetchList(); /* PARAMETERCODE, ALLOWMULTIPLE, VALUE */

        /* Initialize all the customParameters to default values */
        transitionParameters.forEach(parameter -> {
            if("1".equals(parameter.get("ALLOWMULTIPLE")+"")){
                multiples.put((String) parameter.get("PARAMETERCODE"), new LinkedList<String>());
            }else{
                singles.put((String) parameter.get("PARAMETERCODE"), null);
            }
        });

        /* Set the values of the customParameters */
        transitionParameters.forEach(parameter -> {
            if(null != parameter.get("VALUE")){
                if("1".equals(parameter.get("ALLOWMULTIPLE")+"")){
                    multiples.get(parameter.get("PARAMETERCODE")).add((String) parameter.get("VALUE"));
                }else{
                    singles.put((String) parameter.get("PARAMETERCODE"), (String) parameter.get("VALUE"));
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
