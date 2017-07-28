package net.micropact.aea.eu.doe;

import com.entellitrak.ReferenceObjectEventContext;

import net.micropact.aea.core.doe.AReferenceObjectEventHandler;

/**
 * This class is the Data Object Event Handler for the EU Email Queue Status object.
 *
 * @author zachary.miller
 */
public class EuEmailQueueStatusDoe extends AReferenceObjectEventHandler {

    @Override
    protected void executeObject(final ReferenceObjectEventContext etk) {
        // No additional logic
    }
}
