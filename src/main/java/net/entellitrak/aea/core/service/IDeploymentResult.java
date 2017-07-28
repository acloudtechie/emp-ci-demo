package net.entellitrak.aea.core.service;

/**
 * Interface for returning a summary of the result of running component setup.
 *
 * @author Zachary.Miller
 * @see IDeploymentService#runComponentSetup()
 */
public interface IDeploymentResult {

    /**
     * Get a string representation of the summary information of setting up deployment.
     *
     * @return the summary string
     */
    String getSummaryString();
}
