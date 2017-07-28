package net.micropact.aea.rf.doe;

import com.entellitrak.DataObjectEventContext;

import net.micropact.aea.core.doe.ADataObjectEventHandler;
import net.micropact.aea.rf.utility.twoWayMultiselect.TwoWayMultiselectFactory;
import net.micropact.aea.rf.utility.twoWayMultiselect.TwoWayMultiselectUtility;

/**
 * This class is for handling any ETP code that should fire based on the RF State object.
 *
 * The main job of this Script Object is to handle the allowedTransitions checklist, because the checklist itself
 * actually belongs to RfTransition.
 * We must read from the Form Control, and update the database
 *
 * @see net.micropact.aea.rf.formListener.RfStateRead
 * @author zmiller
 */
public final class RfStateDoe extends ADataObjectEventHandler{

    @Override
    protected void executeObject(final DataObjectEventContext etk) throws Exception {
        TwoWayMultiselectUtility.parseUnboundMultiselects(etk,
                TwoWayMultiselectFactory.getRfStateAllowedTransitionsMultiselect(etk));
    }
}
