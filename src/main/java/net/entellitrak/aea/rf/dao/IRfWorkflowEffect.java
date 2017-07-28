package net.entellitrak.aea.rf.dao;

import java.util.List;

import net.entellitrak.aea.rf.ICustomParameters;

/**
 * This is the interface for DAO objects representing RF Workflow Effect data.
 *
 * @author zachary.miller
 */
public interface IRfWorkflowEffect {

    /**
     * Get the Tracking Id.
     *
     * @return the Tracking Id
     */
    long getId();

    /**
     * Get the Name.
     *
     * @return the Name
     */
    String getName();

    /**
     * Get the Code.
     *
     * @return the Code
     */
    String getCode();

    /**
     * Get the Execution Order.
     *
     * @return the Execution Order
     */
    long getExecutionOrder();

    /**
     * Get the RF Script which this effect executes.
     *
     * @return the Script
     * @throws Exception If anything went wrong
     */
    IRfScript getScript() throws Exception;

    /**
     * Get the RF Transitions for which this Workflow Effect will fire.
     *
     * @return the RF Transitions
     * @throws Exception If anything went wrong
     */
    List<IRfTransition> getTransitions() throws Exception;

    /**
     * Get the custom parameters that this Workflow Effect was configured with (the possible parameters are defined in
     * RF Script, the values are provided in RF Workflow Effect).
     *
     * @return the parameters/values
     */
    ICustomParameters getParameters();
}
