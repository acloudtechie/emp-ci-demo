package net.micropact.aea.core.query;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.entellitrak.ExecutionContext;

import net.micropact.aea.utility.Utility;

/**
 * <p>
 *  This class is used to escape special characters in an SQL LIKE clause.
 * </p>
 * <p>
 *  This turns out to be surprisingly difficult because of how SQL Server and Oracle implement this and their
 *  intolerance for escaping more than is necessary and even after reading their documentation I am not sure that this
 *  implementation is completely correct.
 * </p>
 * <p>
 *  To use this class you have a SQL fragment in your query like
 *      <code>WHERE c_column LIKE '%' || :searchText || '%' ESCAPE :escapeChar</code>
 *  and then use <code>.setParameter("searchText", {@link EscapeLike#escapeLike(ExecutionContext, String)})</code>
 *  and <code>.setParameter("escapeChar", {@link EscapeLike#getEscapeCharString()})</code>
 *  to get the query parameters.
 * </p>
 *
 * @author zmiller
 */
public final class EscapeLike {

    private static final Character ESCAPE_CHAR = '-';

    /**
     * Utility classes do not need constructors.
     */
    private EscapeLike(){}

    /**
     * Gets the escape character as a String. The reason for returning the character as a String instead of a Character
     * is that {@link com.entellitrak.SQLFacade#setParameter(String, Object)} does not handle Characters correctly.
     *
     * @return The escape character as a String.
     */
    public static String getEscapeCharString(){
        return ESCAPE_CHAR.toString();
    }

    /**
     * Escapes the special characters in an SQL LIKE clause such as "%", "_" and sometimes "[";.
     *
     * @param etk entellitrak execution context
     * @param string The string which is to be escaped.
     * @return The escaped LIKE clause.
     */
    public static String escapeLike(final ExecutionContext etk, final String string) {
        if (string == null) {
            return "";
        } else {

            /* Oracle and SQL Server have different sets of special characters, and they will not allow you to escape a
             * character which doesn't actually have to be escaped. */
            final List<Character> specialCharacters = Utility.isSqlServer(etk)
                    ? Arrays.asList(ESCAPE_CHAR, '%', '_', '[')
                     : Arrays.asList(ESCAPE_CHAR, '%', '_');

            String returnString = string;
            for(final Character specialCharacter : specialCharacters){
                returnString = returnString.replaceAll(Pattern.quote(specialCharacter.toString()),
                        getEscapeCharString() + specialCharacter.toString());
            }

            return returnString;
        }
    }
}
