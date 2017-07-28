package net.micropact.aea.rf.doe;

import com.entellitrak.DataObjectEventContext;

import net.micropact.aea.core.doe.ADataObjectEventHandler;
import net.micropact.aea.rf.utility.dynamicParameters.DynamicParametersUtility;

/**
 * The Data Object Event Handler for the RF Workflow Parameter object.
 *
 * @author zmiller
 *
 */
public class RfWorkflowParameterDoe extends ADataObjectEventHandler {

    @Override
    protected void executeObject(final DataObjectEventContext etk) throws Exception {
        DynamicParametersUtility.validateLookupField(etk);
    }
}
