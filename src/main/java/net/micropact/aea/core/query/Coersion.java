package net.micropact.aea.core.query;

import java.util.LinkedList;
import java.util.List;

/**
 * This class contains methods for coercing values from queries to certain types of objects.
 *
 * @author zmiller
 */
public final class Coersion {

    /**
     * Utility classes do not need constructors.
     */
    private Coersion(){}

    /**
     * Converts an arbitrary object to a Boolean. Not every type is currently supported. This method can be extended
     * to support more types as necessary.
     *
     * @param object object to convert
     * @return the boolean value of the object
     */
    public static Boolean toBoolean(final Object object) {
        Boolean returnValue;

        if(object == null){
            returnValue = null;
        }else if(object instanceof Boolean){
            returnValue = (Boolean) object;
        }else if(object instanceof Number){
            returnValue = ((Number) object).longValue() != 0;
        }else{
            throw new IllegalArgumentException(
                    String.format("Do not yet now how to convert objects of type \"%s\" to booleans",
                            object.getClass().getName()));
        }

        return returnValue;
    }

    /**
     * Converts an arbitrary object to a boolean. null will be treated as false. Not every type is currently supported.
     * This method can be extended to support more types as necessary.
     *
     * @param object object to convert
     * @return the boolean value
     */
    public static boolean toBooleanNonNull(final Object object){
        final Boolean value = toBoolean(object);
        return value != null && value;
    }

    /**
     * Converts an object to a Long. Not every type is currently supported. This method can be extended to support more
     * types as necessary.
     *
     * @param object object to convert
     * @return the Long value of the object
     */
    public static Long toLong(final Object object){
        final Long returnValue;

        if(object == null){
            returnValue = null;
        }else if(object instanceof Number){
            returnValue = ((Number) object).longValue();
        }else if(object instanceof String){
            returnValue = Long.parseLong((String) object);
        }else{
            throw new IllegalArgumentException(
                    String.format("Do not yet now how to convert objects of type \"%s\" to longs",
                            object.getClass().getName()));
        }

        return returnValue;
    }

    /**
     * Converts an object to a Integer. Not every type is currently supported. This method can be extended to support more
     * types as necessary.
     *
     * @param object object to convert
     * @return the Integer value of the object
     */
    public static Integer toInteger(final Object object){
        return object == null ? null : ((Number) object).intValue();
    }

    /**
     * Coerces a String to a non-empty value. Empty Strings will become null.
     * This is primarily an issue due to code (both AE and core) being inconsistent in how they store null/empty string
     * in SQL Server.
     *
     * @param string The string to coerce
     * @return A non-empty, possibly null string
     */
    public static String toNonEmptyString(final String string) {
        return "".equals(string) ? null : string;
    }

    /**
     * This method converts a list of objects which can be converted to longs via {@link #toLong(Object)}
     * to a list of the longs.
     *
     * @param objects list of objects to convert to longs
     * @return the list of longs
     */
    public static List<Long> toLongs(final List<?> objects) {
        final List<Long> returnList = new LinkedList<>();
        for(final Object object : objects){
            returnList.add(toLong(object));
        }
        return returnList;
    }
}
