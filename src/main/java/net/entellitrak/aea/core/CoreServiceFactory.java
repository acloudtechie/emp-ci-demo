package net.entellitrak.aea.core;

import com.entellitrak.ExecutionContext;

import net.entellitrak.aea.core.service.IDeploymentService;
import net.micropact.aea.core.service.DeploymentService;

/**
 * Factory for accessing instances of services related to the core components.
 *
 * @author Zachary.Miller
 */
public final class CoreServiceFactory {

    /**
     * Utility classes do not need public constructors.
     */
    private CoreServiceFactory(){}

    /**
     * Get an instance of a service for dealing with component deployment functionality.
     *
     * @param etk entellitrak execution context
     * @return an instance of {@link IDeploymentService}
     */
    public static IDeploymentService getDeploymentService(final ExecutionContext etk) {
        return new DeploymentService(etk);
    }
}
