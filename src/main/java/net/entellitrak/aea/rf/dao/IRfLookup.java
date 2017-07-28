package net.entellitrak.aea.rf.dao;

import java.util.Date;

/**
 * This is the interface for DAO classes representing the data for an RF Lookup object.
 *
 * @author zachary.miller
 */
public interface IRfLookup {

    /**
     * Get the Tracking Id of the RF Lookup.
     *
     * @return the Tracking Id
     */
    long getId();

    /**
     * Get the Name of the RF Lookup.
     *
     * @return the Name
     */
    String getName();

    /**
     * Get the Code of the RF Lookup.
     *
     * @return the Code
     */
    String getCode();

    /**
     * Get the Start Date of the RF Lookup.
     *
     * @return the Start Date
     */
    Date getStartDate();

    /**
     * Get the End Date of the RF Lookup.
     *
     * @return the End Date
     */
    Date getEndDate();

    /**
     * Get the SQL used for the RF Lookup.
     *
     * @return the SQL
     */
    String getSQL();
}
