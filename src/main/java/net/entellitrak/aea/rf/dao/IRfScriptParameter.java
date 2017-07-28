package net.entellitrak.aea.rf.dao;

/**
 * This is the interface for DAOs representing RF Script Parameter object data.
 *
 * @author zachary.miller
 */
public interface IRfScriptParameter {

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
     * Gets the Type.
     *
     * @return the Type
     * @throws Exception If anything went wrong
     */
    IRfParameterType getType() throws Exception;

    /**
     * Gets the RF Lookup.
     *
     * @return the RF Lookup
     * @throws Exception if anything went wrong
     */
    IRfLookup getLookup() throws Exception;

    /**
     * Gets whether the parameter is required to have a value.
     *
     * @return whether the parameter is required
     */
    boolean isRequired();

    /**
     * Gets whether the Script Parameter allows multiple values.
     *
     * @return whether the Script Parameter allows multiple values
     */
    boolean isAllowMultiple();

    /**
     * Gets the Code.
     *
     * @return the Code
     */
    String getCode();

    /**
     * Gets the Order.
     *
     * @return the Order
     */
    Long getOrder();

    /**
     * Gets the Description.
     *
     * @return the Description
     */
    String getDescription();
}
