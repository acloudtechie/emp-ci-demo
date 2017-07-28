package net.entellitrak.aea.core.service;

/**
 * Service for handling deployments of component code.
 *
 * @author Zachary.Miller
 */
public interface IDeploymentService {

    /**
     * Runs the setup code for components.
     * This does things such as create database functions and views which are required for components to run.
     * This function needs to be run on each environment that has components installed on it and needs to be run
     * on upgrades as well.
     *
     * @return an object containing information about the result of the setup
     */
    IDeploymentResult runComponentSetup();
}
