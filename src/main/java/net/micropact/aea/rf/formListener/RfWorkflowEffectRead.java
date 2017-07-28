package net.micropact.aea.rf.formListener;

import com.entellitrak.ApplicationException;
import com.entellitrak.form.FormEventContext;
import com.entellitrak.form.FormEventHandler;

import net.micropact.aea.rf.doe.RfWorkflowEffectDoe;
import net.micropact.aea.rf.utility.dynamicParameters.DynamicParametersUseFactory;
import net.micropact.aea.rf.utility.dynamicParameters.DynamicParametersUseFactory.DynamicParameterUsage;
import net.micropact.aea.rf.utility.dynamicParameters.DynamicParametersUtility;

/**
 * This class contains everything that needs to happen for the RF Workflow Effect Form Read Event.
 * The main purpose of this listener is to deal with the RF Script Parameter Values.
 * The RF Script Parameter Values is a child object to RF Workflow Effect but is managed from RF Workflow Effect.
 * This page passes information to the javascript regarding the Script Parameter Values through an unbound form field.
 *
 * @author zmiller
 * @see RfWorkflowEffectDoe
 */
public class RfWorkflowEffectRead implements FormEventHandler {

    @Override
    public void execute(final FormEventContext etk) throws ApplicationException {

        DynamicParametersUtility.initializeParametersElement(etk,
                DynamicParametersUseFactory.loadDynamicParameterUseInfo(etk,
                        DynamicParameterUsage.RF_WORKFLOW_EFFECT_PARAMETER));
    }
}
