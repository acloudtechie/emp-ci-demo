package net.micropact.aea.rf.formListener;

import com.entellitrak.ApplicationException;
import com.entellitrak.form.FormEventContext;
import com.entellitrak.form.FormEventHandler;

import net.micropact.aea.rf.doe.RfTransitionDoe;
import net.micropact.aea.rf.utility.dynamicParameters.DynamicParametersUseFactory;
import net.micropact.aea.rf.utility.dynamicParameters.DynamicParametersUseFactory.DynamicParameterUsage;
import net.micropact.aea.rf.utility.dynamicParameters.DynamicParametersUtility;
import net.micropact.aea.rf.utility.twoWayMultiselect.TwoWayMultiselectFactory;
import net.micropact.aea.rf.utility.twoWayMultiselect.TwoWayMultiselectUtility;

/**
 * This class handles everything that needs to happen when an RF Transition Form is Read.
 * The main purpose is to set the value workflowEffects unbound form field which manages workflowEffects because
 * the data actually belongs to the RF Workflow Effect data object.
 *
 * Now this class is also responsible for setting the values of the previously selected Workflow Parameters on the form.
 *
 * @author zmiller
 * @see RfTransitionDoe
 */
public class RfTransitionRead implements FormEventHandler {

    @Override
    public void execute(final FormEventContext etk) throws ApplicationException {

        TwoWayMultiselectUtility.populateUnboundMultiselect(etk, TwoWayMultiselectFactory.getRfTransitionEffects(etk));

        DynamicParametersUtility.initializeParametersElement(etk,
                DynamicParametersUseFactory.loadDynamicParameterUseInfo(etk,
                        DynamicParameterUsage.RF_TRANSITION_PARAMETER));
    }
}
