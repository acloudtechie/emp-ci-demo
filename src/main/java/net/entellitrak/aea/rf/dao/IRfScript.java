package net.entellitrak.aea.rf.dao;

import java.util.List;

/**
 * This interface represents DAOs for RF Script object data.
 *
 * @author zachary.miller
 */
public interface IRfScript {

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
     * Gets the Script Object which contains the code to execute.
     *
     * @return the Script Object
     */
    IScriptObject getScriptObject();

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
     * Gets the RF Script Parameter objects associated with this RF Script.
     *
     * @return the Script Parameters
     * @throws Exception if anything went wrong
     */
    List<IRfScriptParameter> getScriptParameters() throws Exception;
}
