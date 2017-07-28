package net.micropact.aea.core.doe;

import com.entellitrak.ApplicationException;
import com.entellitrak.ReferenceObjectEventContext;
import com.entellitrak.tracking.ReferenceObjectEventHandler;

/**
 * Abstract class for handling global functionality.
 * reference object event handlers should extend this class.
 *
 * @author Zachary.Miller
 */
public abstract class AReferenceObjectEventHandler implements ReferenceObjectEventHandler {

    @Override
    public final void execute(final ReferenceObjectEventContext etk) throws ApplicationException {
        try {
            BaseObjectEventHandlerUtility.executeCommonCode(etk);
            executeObject(etk);
        } catch (final Exception e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Method to be overridden to perform object-specific actions.
     *
     * @param etk entellitrak execution context
     * @throws Exception If anything goes wrong
     */
    protected abstract void executeObject(final ReferenceObjectEventContext etk) throws Exception;
}
