package net.entellitrak.aea.rf.dao;

import java.util.Date;
import java.util.List;

import com.entellitrak.user.Role;

import net.entellitrak.aea.rf.ITransitionParameters;

/**
 * This is the interface for DAO classes representing RF Transition objects.
 *
 * @author zachary.miller
 */
public interface IRfTransition {

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
     * Gets the Description.
     *
     * @return the Description
     */
    String getDescription();

    /**
     * Gets the To State.
     *
     * @return the To State
     * @throws Exception If anything went wrong
     */
    IRfState getToState() throws Exception;

    /**
     * Returns the From States for this transition.
     *
     * @return the From States
     * @throws Exception if anything went wrong
     */
    List<IRfState> getFromStates() throws Exception;

    /**
     * Gets the Roles which may take this transition.
     *
     * @return the Roles
     */
    List<Role> getRoles();

    /**
     * Gets whether this transition is the initial transition of the workflow.
     *
     * @return whether it is the initial transition
     */
    boolean isInitialTransition();

    /**
     * Gets the Order.
     *
     * @return the Order
     */
    Long getOrder();

    /**
     * Gets the Start Date.
     *
     * @return the Start Date
     */
    Date getStartDate();

    /**
     * Gets the End Date.
     *
     * @return the End Date
     */
    Date getEndDate();

    /**
     * Gets the Workflow Effects which fire when this transition is taken.
     *
     * @return the Workflow Effects
     * @throws Exception if anything went wrong
     */
    List<IRfWorkflowEffect> getWorkflowEffects() throws Exception;

    /**
     * Gets the RF Transition Parameters (including their values) for this RF Transition.
     *
     * @return the Transition Parameter and their values
     */
    ITransitionParameters getTransitionParameters();
}