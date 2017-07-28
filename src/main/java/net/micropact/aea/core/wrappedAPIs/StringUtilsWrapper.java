package net.micropact.aea.core.wrappedAPIs;

import org.apache.commons.lang3.StringUtils;

/**
 * Wrapper for {@link StringUtils}.
 *
 * @author Zachary.Miller
 */
public final class StringUtilsWrapper {

    /**
     * Utility classes do not need public constructors.
     */
    private StringUtilsWrapper(){}

    /**
     * This method replaces all occurrences of searchString with replacementString within text.
     *
     * @param text the text to do replacements upon
     * @param searchString The text which should be replaced
     * @param replacementString the replacement for searchString
     * @return the replaced string
     */
    public static String replace(final String text, final String searchString, final String replacementString) {
        return StringUtils.replace(text, searchString, replacementString);
    }

    /**
     * Checks that a String only contains unicode digits. This means that decimal points, minus signs, etc will
     * cause it to return false.
     *
     * @param string the string to check
     * @return whether the string contains only digits
     */
    public static boolean isNumeric(final String string) {
        return StringUtils.isNumeric(string);
    }

    /**
     * Wrapper around {@link StringUtils#repeat(String, int)}.
     *
     * @param string the string
     * @param times the number of times
     * @return the generated string
     */
    public static String repeat(final String string, final int times) {
        return StringUtils.repeat(string, times);
    }

    /**
     * Wrapper around {@link StringUtils#contains(CharSequence, CharSequence)}.
     *
     * @param seq String to search within
     * @param searchSeq String to search for
     * @return Whether seq contains searchSeq
     */
    public static boolean contains(final String seq, final String searchSeq) {
        return StringUtils.contains(seq, searchSeq);
    }
}
