package net.entellitrak.aea.rf.dao;

import java.util.Date;
import java.util.List;

/**
 * This is the interface for Daos representing RF State object data.
 *
 * @author zachary.miller
 */
public interface IRfState {

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
     * Gets the Order.
     *
     * @return the Order
     */
    Long getOrder();

    /**
     * Gets the End Date.
     *
     * @return the End Date
     */
    Date getEndDate();

    /**
     * Gets the Start Date.
     *
     * @return the Start Date
     */
    Date getStartDate();

    /**
     * Gets the transitions which are allowed to be taken from this RF State.
     *
     * @return the allowed transitions
     * @throws Exception if anything went wrong
     */
    List<IRfTransition> getAllowedTransitions() throws Exception;
}
