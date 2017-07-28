package net.entellitrak.aea.rf;

import net.entellitrak.aea.rf.dao.IRfState;
import net.entellitrak.aea.rf.dao.IRfTransition;
import net.entellitrak.aea.rf.dao.IRfWorkflow;

/**
 * <p>
 *  Interface representing the default parameters which are accessible to every {@link IScript}.
 *  The most important parameter is {@link #getChildTrackingId()} however others such as
 *  {@link #getRfTransition()} are useful for generic scripts which are not tied to particular objects or workflows
 * </p>
 *
 * @author zmiller
 */
public interface IDefaultParameters {

    /**
     * The trackingId of the Child Object which has triggered this effect.
     *
     * @return the trackingId of the Child Object.
     */
    long getChildTrackingId();

    /**
     * The trackingId of the Parent Object which this effect is being triggered for.
     *
     * @return the trackingId of the Parent Object.
     */
    long getParentTrackingId();

    /**
     * Gets the RF Workflow object that the parameters apply to.
     *
     * @return The RF Workflow
     */
    IRfWorkflow getRfWorkflow();

    /**
     * Gets the RF Transition which is being fired.
     *
     * @return The RF Transition
     */
    IRfTransition getRfTransition();

    /**
     * Gets the RF State that the object is coming from. Returns <code>null</code> for the initial transition.
     *
     * @return The RF State that the object is coming from, or null.
     */
    IRfState getFromState();

    /**
     * This method gets the To State of the transition which is being taken. Note that this may be null if the
     * transition does not have a To State selected. If this is not what you want see {@link #getNextState()}
     *
     * @return The RF State, or null
     * @see #getNextState()
     */
    IRfState getToState();

    /**
     * Gets the RF State that the object will be in once the transition is finished. Note that this differs from
     * {@link #getNextState()} in that this method does not return null if the To State is null.
     *
     * @return The next State of the parent object.
     */
    IRfState getNextState();

    /**
     * The code of the RF Workflow that this effect is a part of.
     *
     * @return The Code of the RF Workflow
     * @deprecated Use {@link #getRfWorkflow()}
     */
    @Deprecated
    String getWorkflowCode();

    /**
     * This returns the name of the database table of the Child Object for the RF Workflow.
     *
     * @return The name of the databale table of the Child Object for the RF Workflow.
     * @deprecated Use {@link #getRfWorkflow()}
     */
    @Deprecated
    String getChildTable();

    /**
     * This returns the name of the database column of the Child Object Transition element for the RF Workflow.
     *
     * @return The name of the database column of the Child Object Transition element for the RF Workflow.
     * @deprecated Use {@link #getRfWorkflow()}
     */
    @Deprecated
    String getTransitionColumn();

    /**
     * This returns the name of the database table of the Parent Object for the RF Workflow.
     *
     * @return The name of the database table of the Parent Object for the RF Workflow.
     * @deprecated Use {@link #getRfWorkflow()}
     */
    @Deprecated
    String getParentTable();

    /**
     * This returns the name of the database column for the Parent Object State element for the RF Workflow.
     *
     * @return The name of the database column for the Parent Object State element for the RF Workflow.
     * @deprecated Use {@link #getRfWorkflow()}
     */
    @Deprecated
    String getParentStateColumn();

    /**
     * Get the Code of the RF Transition that the object is currently taking.
     *
     * @return The code of the RF Transition which is being taken.
     * @deprecated Use {@link #getRfTransition()}
     */
    @Deprecated
    String getTransitionCode();

    /**
     * Get the tracking id of the RF Transition that the object is currently taking.
     *
     * @return The tracking id of the RF Transition which is being taken.
     * @deprecated Use {@link #getRfTransition()}
     */
    @Deprecated
    long getTransitionId();

    /**
     * Get the Code of the RF State which the object was in before the current transition was fired.
     *
     * @return The Code of the RF State that the Parent Object is coming from.
     *      If this is the initial transition it returns null.
     * @deprecated Use {@link #getFromState()}
     */
    @Deprecated
    String getFromStateCode();

    /**
     * Get the tracking id of the RF State that the object was in before the current transition was fired.
     *
     * @return The tracking id of the RF State that the Parent Object is coming from.
     *      If this is the initial transition it returns null.
     * @deprecated Use {@link #getFromState()}
     */
    @Deprecated
    Long getFromStateId();

    /**
     * Get the Code of the To State of the RF Transition which is being fired. Note that this may be null.
     *
     * @return The Code of the To State for the RF Transition which is being taken.
     *      Note that the To State can be null.
     *      In the cases where that is not what you want see {@link IDefaultParameters#getNextStateCode()}.
     * @see IDefaultParameters#getNextStateCode()
     * @deprecated Use {@link #getToState()}
     */
    @Deprecated
    String getToStateCode();

    /**
     * Get the tracking id of the To State for the RF Transition which is being taken. Note that this may be null.
     *
     * @return The tracking id of the To State for the RF Transition which is being taken.
     *      Note that the To State can be null.
     *      In the cases where that is not what you want see {@link IDefaultParameters#getNextStateId()}.
     * @see IDefaultParameters#getNextStateId()
     * @deprecated Use {@link #getToState()}
     */
    @Deprecated
    Long getToStateId();

    /**
     * Get the tracking id of the RF State that the object will be in after the current RF Transition is done firing.
     * Note that this method will never return null.
     *
     * @return The tracking id of the RF State that the parent object will be in after this transition is taken.
     *      Note that this method accounts for when the To State is null (the object stays in its current state)
     *      while {@link IDefaultParameters#getToStateId()} does not.
     * @see IDefaultParameters#getToStateId()
     * @deprecated Use {@link #getNextState()}
     */
    @Deprecated
    long getNextStateId();

    /**
     * Get the Code of the RF State that the object will be in after the current RF Transition is done firing.
     * Note that this will never return null.
     *
     * @return The Code of the RF State that the parent object will be in after this transition is taken.
     *      Note that this method accounts for when the To State is null (the object stays in its current state)
     *      while {@link IDefaultParameters#getToStateCode()} does not.
     * @see IDefaultParameters#getToStateCode()
     * @deprecated Use {@link #getNextState()}
     */
    @Deprecated
    String getNextStateCode();
}
