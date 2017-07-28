package net.micropact.aea.rf.doe;

import com.entellitrak.ReferenceObjectEventContext;
import com.entellitrak.tracking.ReferenceObjectEventHandler;

import net.micropact.aea.core.doe.AReferenceObjectEventHandler;

/**
 * {@link ReferenceObjectEventHandler} for the RF Lookup object.
 *
 * @author zachary.miller
 */
public class RfLookupDoe extends AReferenceObjectEventHandler {

    @Override
    protected void executeObject(final ReferenceObjectEventContext etk) {
        // No additional logic
    }
}
