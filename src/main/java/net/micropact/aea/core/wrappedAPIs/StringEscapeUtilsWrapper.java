package net.micropact.aea.core.wrappedAPIs;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * This class wraps {@link StringEscapeUtils}.
 *
 * @author Zachary.Miller
 */
public final class StringEscapeUtilsWrapper {

    /**
     * Utility classes do not need public constructors.
     */
    private StringEscapeUtilsWrapper(){}

    /**
     * Escapes HTML special characters.
     *
     * @param html the string to escape
     * @return the escaped string
     */
    public static String escapeHtml(final String html) {
        return StringEscapeUtils.escapeHtml(html);
    }

    /**
     * Escapes JavaScript special characters.
     *
     * @param javascript the string to escape
     * @return the escaped string
     */
    public static String escapeJavaScript(final String javascript) {
        return StringEscapeUtils.escapeJavaScript(javascript);
    }

    /**
     * Escapes SQL special characters.
     *
     * @param sql the string to escape
     * @return the escaped string
     */
    public static String escapeSql(final String sql) {
        return StringEscapeUtils.escapeSql(sql);
    }

    /**
     * Escapes Java special characters.
     *
     * @param java the string to escape
     * @return the escaped string
     */
    public static String escapeJava(final String java) {
        return StringEscapeUtils.escapeJava(java);
    }
}
