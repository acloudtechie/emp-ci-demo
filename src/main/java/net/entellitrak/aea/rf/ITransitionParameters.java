package net.entellitrak.aea.rf;

import java.util.List;

/**
 * This interface represents the custom parameters that an RF Transition takes.
 * These parameters are defined in RF Workflow Parameter and get their values from RF Transition.
 *
 * @author zmiller
 */
public interface ITransitionParameters {

    /**
     * This method is for returning a parameter which has been marked Allow Multiple = No.
     * It will never return an empty String. If the parameter has not been e8ntered, it will return null.
     *
     * @param parameterCode the Code of the RF Script Parameter object in entellitrak
     * @return the value of the parameter
     */
    String getSingle(String parameterCode);

    /**
     * This method is for returning a parameter which has been marked Allow Multiple = Yes.
     * It will never return null. If no parameters have been entered, it will return an empty list instead.
     *
     * @param parameterCode The Code column of the RF Script Parameter object in entellitrak.
     * @return the values of the parameter
     */
    List<String> getMultiple(String parameterCode);
}
