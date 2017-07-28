package net.micropact.aea.core.utility;

import net.micropact.aea.core.wrappedAPIs.StringEscapeUtilsWrapper;

/**
 * This class contains useful functions for escaping Strings.
 *
 * @author Zachary.Miller
 */
public final class StringEscapeUtils {

    /**
     * Utility classes do not need public constructors.
     */
    private StringEscapeUtils(){}

    /**
     * Escapes HTML special characters.
     *
     * @param html the string to escape
     * @return the escaped string
     */
    public static String escapeHtml(final String html) {
        return StringEscapeUtilsWrapper.escapeHtml(html);
    }

    /**
     * Escapes JavaScript special characters.
     *
     * @param javascript the string to escape
     * @return the escaped string
     */
    public static String escapeJavaScript(final String javascript) {
        return StringEscapeUtilsWrapper.escapeJavaScript(javascript);
    }

    /**
     * Escapes SQL special characters.
     *
     * @param sql the string to escape
     * @return the escaped string
     */
    public static String escapeSql(final String sql) {
        return StringEscapeUtilsWrapper.escapeSql(sql);
    }

    /**
     * Escapes Java special characters.
     *
     * @param java the string to escape
     * @return the escaped string
     */
    public static String escapeJava(final String java) {
        return StringEscapeUtilsWrapper.escapeJava(java);
    }
}
