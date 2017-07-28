package net.micropact.aea.du.applyChanges;

import com.entellitrak.ApplicationException;
import com.entellitrak.system.ApplyChangesEventHandler;
import com.entellitrak.system.ApplyChangesExecutionContext;

/**
 * This Apply Changes handler clears the AE Cache.
 * In a high concurrency system <strong>you still may need to clear the cache manually</strong>.
 * This is the case if your cache contains information dependent on the tracking configuration because a user could load
 * data into the cache after it has been cleared, but before the Apply Changes transaction has committed.
 *
 * @author zmiller
 */
public class ClearCache implements ApplyChangesEventHandler {

    @Override
    public void execute(final ApplyChangesExecutionContext etk) throws ApplicationException {
        etk.getCache().clearCache();
    }
}
