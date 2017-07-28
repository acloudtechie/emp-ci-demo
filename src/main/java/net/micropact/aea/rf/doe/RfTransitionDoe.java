package net.micropact.aea.rf.doe;

import com.entellitrak.DataObjectEventContext;

import net.micropact.aea.core.doe.ADataObjectEventHandler;
import net.micropact.aea.rf.utility.dynamicParameters.DynamicParametersUseFactory;
import net.micropact.aea.rf.utility.dynamicParameters.DynamicParametersUseFactory.DynamicParameterUsage;
import net.micropact.aea.rf.utility.dynamicParameters.DynamicParametersUtility;
import net.micropact.aea.rf.utility.twoWayMultiselect.TwoWayMultiselectFactory;
import net.micropact.aea.rf.utility.twoWayMultiselect.TwoWayMultiselectUtility;

/**
 * This class handles all functionality related to the ETP of an RF Transition.
 *
 * The main job of this Script Object is to handle the workflowEffects checklist, because the checklist itself actually
 * belongs to RfWorkflowEffect.
 * We must read from the Form Control, and update the database
 *
 * @see net.micropact.aea.rf.formListener.RfTransitionRead
 *
 * @author zmiller
 */
public final class RfTransitionDoe extends ADataObjectEventHandler{

    @Override
    protected void executeObject(final DataObjectEventContext etk) throws Exception {
        TwoWayMultiselectUtility.parseUnboundMultiselects(etk,
                TwoWayMultiselectFactory.getRfTransitionEffects(etk));

        DynamicParametersUtility.saveParameters(etk,
                etk.getNewObject().properties().getParentId(),
                DynamicParametersUseFactory.loadDynamicParameterUseInfo(etk,
                        DynamicParameterUsage.RF_TRANSITION_PARAMETER));

        /* Since our only child is hidden, we are going to redirect to the listing screen because it's convenient
         * (and that's how the rest of entellitrak works) */
        etk.getRedirectManager()
        .redirectToList("object.rfTransition", etk.getNewObject().properties().getParentId());
    }
}
