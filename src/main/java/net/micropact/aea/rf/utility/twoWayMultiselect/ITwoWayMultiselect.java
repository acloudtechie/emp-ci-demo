package net.micropact.aea.rf.utility.twoWayMultiselect;

import com.entellitrak.configuration.DataElement;

/**
 * This class represents the data which one needs to know in order to manage a two-way multiselect.
 *
 * @author zmiller
 */
public interface ITwoWayMultiselect {

    /**
     * Get the unbound form field which is used to manage the multiselect.
     *
     * @return The unbound form field which is used to manage the multiselect from its secondary location.
     */
    String getFieldName();

    /**
     * Get the data element which the data is actually stored in.
     *
     * @return The data element which the data is actually stored in.
     */
    DataElement getReferencedElement();
}
