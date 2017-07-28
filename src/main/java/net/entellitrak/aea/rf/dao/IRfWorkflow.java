package net.entellitrak.aea.rf.dao;

import java.util.List;

import com.entellitrak.configuration.DataElement;

/**
 * This is the interface for DAOs for RF Workflow object data.
 *
 * @author zachary.miller
 */
public interface IRfWorkflow {

    /**
     * Gets the Tracking Id.
     *
     * @return the Tracking Id
     */
    long getId();

    /**
     * Gets the Name.
     *
     * @return the Name
     */
    String getName();

    /**
     * Gets the Code.
     *
     * @return the Code
     */
    String getCode();

    /**
     * Gets the Child Object's transition element.
     *
     * @return the Data Element
     */
    DataElement getChildTransitionElement();

    /**
     * Gets the Parent Object's state element.
     *
     * @return the Data Element
     */
    DataElement getParentStateElement();

    /**
     * Gets the Description.
     *
     * @return the Description
     */
    String getDescription();

    /**
     * Gets the RF States in this workflow.
     *
     * @return the RF States
     * @throws Exception If anything went wrong
     */
    List<IRfState> getStates() throws Exception;

    /**
     * Gets all of the RF Transitions within this workflow.
     *
     * @return the RF Transitions
     * @throws Exception If anything went wrong
     */
    List<IRfTransition> getTransitions() throws Exception;

    /**
     * Gets all of the RF Workflow Effects within this workflow.
     *
     * @return the RF Workflow Effects
     * @throws Exception if anything went wrong
     */
    List<IRfWorkflowEffect> getWorkflowEffects() throws Exception;

    /**
     * Gets the Workflow Parameters for this workflow.
     *
     * @return the RF Workflow Parameters
     * @throws Exception if anything went wrong
     */
    List<IRfWorkflowParameter> getWorkflowParameters() throws Exception;
}
