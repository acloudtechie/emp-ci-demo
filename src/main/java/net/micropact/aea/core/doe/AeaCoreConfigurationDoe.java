package net.micropact.aea.core.doe;

import com.entellitrak.ReferenceObjectEventContext;

import net.entellitrak.aea.core.cache.CacheManager;
import net.micropact.aea.core.cache.AeaCoreConfigurationCacheable;

/**
 * Data Object Event Handler for the AEA Core Configuration object.
 *
 * @author zachary.miller
 */
public class AeaCoreConfigurationDoe extends AReferenceObjectEventHandler {

    @Override
    protected void executeObject(final ReferenceObjectEventContext etk) {
        clearRelevantCache(etk);
    }

    /**
     * Clears the caches which may have been invalidated by saving of the AEA Core Configuration object.
     *
     * @param etk entellitrak execution context.
     */
    private static void clearRelevantCache(final ReferenceObjectEventContext etk) {
        etk.getResult().addMessage("The values within the AEA CORE Configuration reference table are cached. Although the cache has just been cleared it is possible that another user has loaded old values back into the cache. If the site appears to continue to use the old value, you must manually clear the cache for the changes to take effect.");
        CacheManager.remove(etk, new AeaCoreConfigurationCacheable(etk));
    }
}
