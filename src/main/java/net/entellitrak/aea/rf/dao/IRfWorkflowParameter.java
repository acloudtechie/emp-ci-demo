package net.entellitrak.aea.rf.dao;

/**
 * The interface for DAO objects representing the RF Workflow Parameter object data.
 *
 * @author zachary.miller
 */
public interface IRfWorkflowParameter {

    /**
     * Get the Tracking Id of the RF Workflow Parameter.
     *
     * @return the Tracking Id
     */
    long getId();

    /**
     * Get the Name of the Workflow Parameter.
     *
     * @return the Name
     */
    String getName();

    /** Get the RF Parameter Type of the Workflow Parameter.
     * @return the Parameter Type
     * @throws Exception If anything went wrong.
     */
    IRfParameterType getParameterType() throws Exception;

    /**
     * Get the RF Lookup of the Workflow Parameter.
     *
     * @return the RF Lookup
     * @throws Exception If anything went wrong.
     */
    IRfLookup getRfLookup() throws Exception;

    /**
     * Get whether the Workflow Parameter is required.
     *
     * @return if the Workflow Parameter is required
     */
    boolean isRequired();

    /**
     * Get whether the Workflow Parameter allows multiple values.
     *
     * @return if the Workflow Parameter allows multiple values
     */
    boolean isAllowMultiple();

    /**
     * Get the Code of the Workflow Parameter.
     *
     * @return the Code
     */
    String getCode();

    /**
     * Get the Order of the Workflow Parameter.
     *
     * @return the Order
     */
    Long getOrder();

    /**
     * Get the Description of the Workflow Parameter.
     *
     * @return the Description
     */
    String getDescription();
}
