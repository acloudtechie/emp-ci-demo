package net.micropact.aea.du.utility.systemPreference;

import java.util.Optional;

/**
 * DAO class representing a System Preference and its corresponding value.
 *
 * @author zmiller
 */
public class SystemPreferenceValue {
    private final String name;
    private final Optional<String> value;

    /**
     * Constructs a new System Preference value.
     *
     * @param theName name of the System Preference
     * @param theValue Value of the System Preference
     */
    public SystemPreferenceValue(final String theName, final Optional<String> theValue){
        name = theName;
        value = theValue;
    }

    /**
     * Get the name of the System Preference.
     *
     * @return the name of the System Preference
     */
    public String getName(){
        return name;
    }

    /**
     * Get the value of the System Preference.
     *
     * @return The value of the system preference. Returns an empty optional if the System Preference does not exist
     *          in the system.
     */
    public Optional<String> getValue(){
        return value;
    }
}