package net.entellitrak.aea.du;

import com.entellitrak.ExecutionContext;

import net.entellitrak.aea.du.service.ISystemPreferenceMigrationService;
import net.micropact.aea.du.service.SystemPreferenceMigrationService;

/**
 * This class is part of the public API to grant access to any Services related to the Developer Utility in much the
 * same way that the ExecutionContext in entellitrak is used to access most other core services.
 *
 * @author zachary.miller
 */
public final class DuServiceFactory {

    /**
     * Utility classes do not need public constructors.
     */
    private DuServiceFactory(){}

    /**
     * Get access to a service for migrating system preferences from one site to another.
     *
     * @param etk entellitrak execution context
     * @return An {@link ISystemPreferenceMigrationService}
     */
    public static ISystemPreferenceMigrationService getSystemPreferenceMigrationService(final ExecutionContext etk){
        return new SystemPreferenceMigrationService(etk);
    }
}
