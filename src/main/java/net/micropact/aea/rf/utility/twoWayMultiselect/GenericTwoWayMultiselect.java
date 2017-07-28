package net.micropact.aea.rf.utility.twoWayMultiselect;

import com.entellitrak.ExecutionContext;
import com.entellitrak.configuration.DataElement;

/**
 * Convenience class for easily representing {@link ITwoWayMultiselect}.
 * It can represent any data element/unbound form field combination.
 *
 * @author zmiller
 */
public class GenericTwoWayMultiselect implements ITwoWayMultiselect {

    private final String fieldName;
    private final DataElement referencedElement;

    /**
     * {@link GenericTwoWayMultiselect} constructor.
     *
     * @param etk entellitrak execution context
     * @param theReferencedElement The business key of the data element storing the actual data.
     * @param theFieldName The name of the unbound form field
     */
    public GenericTwoWayMultiselect(final ExecutionContext etk, final String theReferencedElement, final String theFieldName){
        fieldName = theFieldName;
        referencedElement = etk.getDataElementService().getDataElementByBusinessKey(theReferencedElement);
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public DataElement getReferencedElement() {
        return referencedElement;
    }
}
