package net.micropact.aea.rf.utility.dynamicParameters;

import com.entellitrak.ExecutionContext;

/**
 * Class for generating the necessary instances of {@link IDynamicParameterUseInfo}. Those are places within the
 * Rules Framework which use the dynamic parameters pattern.
 *
 * @author zmiller
 */
public final class DynamicParametersUseFactory {

    /**
     * This enum contains information about each of the places which utilizes dynamic parameters.
     *
     * @author zmiller
     */
    public enum DynamicParameterUsage{
        /**
         * This represents the Parameters on the RF Transition screen.
         */
        RF_TRANSITION_PARAMETER("object.rfWorkflowParameter",
                "object.rfTransitionParameterValue.element.workflowParameter"),
        /**
         * This represents the Parameters on the RF Workflow Effect screen.
         */
        RF_WORKFLOW_EFFECT_PARAMETER("object.rfScriptParameter",
                "object.rfScriptParameterValue.element.scriptParameter");

        private final String parameterDefiningObjectKey;
        private final String parameterIdColumnKey;

        /**
         * Constructor for {@link DynamicParameterUsage}.
         *
         * @param parameterDefiningObjectBusinessKey The business key of the data object which defines the parameters
         *          (ex: "object.rfScriptParameter")
         * @param parameterIdColumnBusinessKey The business key of the data element which contains the reference to the
         *          parameter object. (ex: "object.rfScriptParameterValue.element.scriptParameter")
         */
        DynamicParameterUsage(final String parameterDefiningObjectBusinessKey,
                final String parameterIdColumnBusinessKey){
            parameterDefiningObjectKey = parameterDefiningObjectBusinessKey;
            parameterIdColumnKey = parameterIdColumnBusinessKey;
        }

        /**
         * Gets the business key of the data object which defines the parameters.
         *
         * @return The business key.
         */
        String getParameterDefiningObjectBusinessKey(){
            return parameterDefiningObjectKey;
        }

        /**
         * Gets the business key of the data element which contains the reference to the parameter object.
         *
         * @return The business key.
         */
        String getParameterIdColumnBusinessKey(){
            return parameterIdColumnKey;
        }
    }

    /**
     * Utility classes do not need constructors.
     */
    private DynamicParametersUseFactory(){}

    /**
     * Returns the metadata for a particular parameter usage.
     *
     * @param etk entellitrak execution context
     * @param parameterUsage The parameter usage which describes where the parameter is used
     * @return The metadata for the particular parameter usage
     */
    public static IDynamicParameterUseInfo loadDynamicParameterUseInfo(final ExecutionContext etk,
            final DynamicParameterUsage parameterUsage){
        return new GenericDynamicParametersUse(etk, parameterUsage.getParameterDefiningObjectBusinessKey(),
                parameterUsage.getParameterIdColumnBusinessKey());
    }
}
