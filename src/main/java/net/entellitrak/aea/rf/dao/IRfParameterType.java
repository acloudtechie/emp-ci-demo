package net.entellitrak.aea.rf.dao;

/**
 * The interface for DAOs which represent RF Parameter Type data.
 *
 * @author zachary.miller
 */
public interface IRfParameterType {

    /**
     * Get the Tracking Id.
     *
     * @return the Tracking Id
     */
    long getId();

    /**
     * Get the Name.
     *
     * @return The Name
     */
    String getName();

    /**
     * Get the Code.
     *
     * @return The Code
     */
    String getCode();

    /**
     * Get the Order.
     *
     * @return The Order
     */
    long getOrder();
}
