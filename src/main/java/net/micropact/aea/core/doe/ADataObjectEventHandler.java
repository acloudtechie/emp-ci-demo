package net.micropact.aea.core.doe;

import com.entellitrak.ApplicationException;
import com.entellitrak.DataObjectEventContext;
import com.entellitrak.tracking.DataObjectEventHandler;

/**
 * Abstract class for implementing {@link DataObjectEventHandler}s which share site-wide code.
 *
 * @author Zachary.Miller
 */
public abstract class ADataObjectEventHandler implements DataObjectEventHandler {

    @Override
    public final void execute(final DataObjectEventContext etk) throws ApplicationException {
        try {
            BaseObjectEventHandlerUtility.executeCommonCode(etk);
            executeObject(etk);
        } catch (final Exception e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Execute the object-specific code.
     *
     * @param etk entellitrak execution context
     * @throws Exception If there is an underlying {@link Exception}
     */
    protected abstract void executeObject(DataObjectEventContext etk) throws Exception;
}
