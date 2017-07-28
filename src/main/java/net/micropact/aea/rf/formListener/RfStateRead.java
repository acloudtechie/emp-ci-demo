package net.micropact.aea.rf.formListener;

import com.entellitrak.ApplicationException;
import com.entellitrak.form.FormEventContext;
import com.entellitrak.form.FormEventHandler;

import net.micropact.aea.rf.doe.RfStateDoe;
import net.micropact.aea.rf.utility.twoWayMultiselect.TwoWayMultiselectFactory;
import net.micropact.aea.rf.utility.twoWayMultiselect.TwoWayMultiselectUtility;

/**
 * This listener handles everything that needs to happen when an RF State Form is Read.
 * The reason that this listener is needed is to setup the transitions checklist,
 * because the checklist itself actually belongs to rfTransition
 *
 * @author zmiller
 * @see RfStateDoe
 */
public class RfStateRead implements FormEventHandler {

    @Override
    public void execute(final FormEventContext etk) throws ApplicationException {
        TwoWayMultiselectUtility.populateUnboundMultiselect(etk,
                TwoWayMultiselectFactory.getRfStateAllowedTransitionsMultiselect(etk));
    }
}
