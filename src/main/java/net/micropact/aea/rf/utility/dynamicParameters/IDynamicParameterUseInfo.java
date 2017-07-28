package net.micropact.aea.rf.utility.dynamicParameters;

import com.entellitrak.configuration.DataElement;
import com.entellitrak.configuration.DataObject;

/**
 * Class which contains the information needed to manage a particular instance of the dynamic parameters scheme
 * which is used for Workflow Effect scripts as well as Transition Parameters.
 *
 * @author zmiller
 */
public interface IDynamicParameterUseInfo{

    /**
     * Gets the data element which stores the reference to the parameter definition.
     *
     * @return The data element which stores the reference to the parameter definition.
     */
    DataElement getParameterReferenceElement();

    /**
     * Gets the table which contains definitions of the parameters.
     *
     * @return The table which contains definitions of the parameters.
     */
    DataObject getParameterDefiningObject();
}