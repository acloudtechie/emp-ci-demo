package net.entellitrak.aea.rf;

import java.util.List;

/**
 * Interface representing the custom parameters that an RF Script takes.
 * These parameters are defined in RF Script Parameter and get their values from RF Workflow Effect.
 *
 * @author zmiller
 */
public interface ICustomParameters {

    /**
     * Method for fetching the value of a parameter which has been marked Allow Multiple = No.
     * It never returns an empty String.
     * If the parameter has not been entered, this method returns null.
     *
     * @param parameterCode Code of the RF Script Parameter object in entellitrak
     * @return the value of the parameter
     */
    String getSingle(String parameterCode);

    /**
     * Method for fetching the value of a parameter which has been marked Allow Multiple = Yes.
     * It never returns <code>null</code>. If no parameters have been entered, an empty list is returned.
     *
     * @param parameterCode Code column of the RF Script Parameter object in entellitrak.
     * @return the values of the parameter
     */
    List<String> getMultiple(String parameterCode);
}
