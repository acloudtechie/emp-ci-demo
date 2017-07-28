package net.micropact.aea.core.wrappedAPIs;

import org.apache.commons.lang.ObjectUtils;

/**
 * Wrapper around {@link ObjectUtils}.
 *
 * @author Zachary.Miller
 */
public final class ObjectUtilsWrapper {

    /**
     * Utility classes do not need public constructors.
     */
    private ObjectUtilsWrapper(){}

    /**
     * Get the toString of an object, but provide a default value for if the object is null.
     *
     * @param object the object to convert to a String
     * @param defaultValue the value to return if the object was null
     * @return the string representation of the object
     */
    public static String toString(final Object object, final String defaultValue) {
        return ObjectUtils.toString(object, defaultValue);
    }

    /**
     * Determine whether 2 objects are equal.
     *
     * @param object1 first object
     * @param object2 second object
     * @return whether the two objects are equal
     */
    public static boolean equals(final Object object1, final Object object2) {
        return ObjectUtils.equals(object1, object2);
    }
}
