package net.micropact.aea.rf.utility.dynamicParameters;

import com.entellitrak.ExecutionContext;
import com.entellitrak.configuration.DataElement;
import com.entellitrak.configuration.DataObject;

/**
 * Convenience class for easily representing {@link IDynamicParameterUseInfo}.
 *
 * @author zmiller
 */
public class GenericDynamicParametersUse implements IDynamicParameterUseInfo {

    private final DataElement parameterReferenceElement;
    private final DataObject parameterDefiningObject;

    /**
     * {@link GenericDynamicParametersUse} constructor.
     *
     * @param etk entellitrak execution context
     * @param parameterDefiningObjectBusinessKey The business key of the object which defines the parameters
     * @param parameterIdColumnBusinessKey The business key of the data element which stores the reference to the object
     *          defining the parameters.
     */
    public GenericDynamicParametersUse(final ExecutionContext etk,
            final String parameterDefiningObjectBusinessKey,
            final String parameterIdColumnBusinessKey){
        parameterReferenceElement = etk.getDataElementService().getDataElementByBusinessKey(parameterIdColumnBusinessKey);
        parameterDefiningObject = etk.getDataObjectService().getDataObjectByBusinessKey(parameterDefiningObjectBusinessKey);
    }

    @Override
    public DataElement getParameterReferenceElement() {
        return parameterReferenceElement;
    }

    @Override
    public DataObject getParameterDefiningObject() {
        return parameterDefiningObject;
    }
}
