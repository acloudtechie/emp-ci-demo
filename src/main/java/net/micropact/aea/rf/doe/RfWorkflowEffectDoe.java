package net.micropact.aea.rf.doe;

import com.entellitrak.DataObjectEventContext;
import com.entellitrak.dynamic.RfWorkflowEffect;

import net.micropact.aea.core.doe.ADataObjectEventHandler;
import net.micropact.aea.rf.utility.dynamicParameters.DynamicParametersUseFactory;
import net.micropact.aea.rf.utility.dynamicParameters.DynamicParametersUseFactory.DynamicParameterUsage;
import net.micropact.aea.rf.utility.dynamicParameters.DynamicParametersUtility;

/**
 * This class is responsible for handling everything related to the ETP for an RF Workflow Effect object.
 *
 * @author zmiller
 */
public final class RfWorkflowEffectDoe extends ADataObjectEventHandler {

    @Override
    protected void executeObject(final DataObjectEventContext etk) throws Exception {
        final RfWorkflowEffect rfWorkflowEffect = (RfWorkflowEffect) etk.getNewObject();
        final long workflowId = rfWorkflowEffect.properties().getBaseId();

        DynamicParametersUtility.saveParameters(etk,
                rfWorkflowEffect.getScript(),
                DynamicParametersUseFactory.loadDynamicParameterUseInfo(etk,
                        DynamicParameterUsage.RF_WORKFLOW_EFFECT_PARAMETER));

        /* Since our only child is hidden, we are going to redirect to the listing screen because it's convenient
         * (and that's how the rest of entellitrak works) */
        etk.getRedirectManager().redirectToList("object.rfWorkflowEffect", new Long(workflowId));
    }
}
