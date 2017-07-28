package net.micropact.aea.core.utility;

import java.util.List;

import net.micropact.aea.core.wrappedAPIs.StringUtilsWrapper;

/**
 * This class contains useful functions related to the {@link String} class.
 *
 * @author Zachary.Miller
 */
public final class StringUtils {

    /**
     * Utility classes do not need public constructors.
     */
    private StringUtils(){}

    /** Joins a list of Strings with a given separator.
     *
     * @param strings Strings to join
     * @param separator item to interpose between the strings
     * @return the separator-separated string
     */
    public static String join(final List<String> strings, final String separator) {
        String returnString;

        if(strings.isEmpty()){
            returnString = "";
        }else{
            final StringBuilder builder = new StringBuilder();
            for(final String string : strings){
                builder.append(separator).append(string);
            }
            returnString = builder.substring(separator.length());
        }
        return returnString;
    }

    /**
     * This method replaces all occurrences of searchString with replacementString within text.
     *
     * @param text the text to do replacements upon
     * @param searchString The text which should be replaced
     * @param replacementString the replacement for searchString
     * @return the replaced string
     */
    public static String replace(final String text, final String searchString, final String replacementString) {
        return StringUtilsWrapper.replace(text, searchString, replacementString);
    }

    /**
     * Checks that a String only contains unicode digits. This means that decimal points, minus signs, etc will
     * cause it to return false.
     *
     * @param string the string to check
     * @return whether the string contains only digits
     */
    public static boolean isNumeric(final String string) {
        return StringUtilsWrapper.isNumeric(string);
    }

    /**
     * Generates a String which is formed by repeating a particular string a number of times.
     *
     * @param string The string to repeat
     * @param times The times to repeat
     * @return the generated String
     */
    public static String repeat(final String string, final int times) {
        return StringUtilsWrapper.repeat(string, times);
    }

    /**
     * Checks whether a string is contained within another. Does not error when passed null values.
     *
     * @param seq The String which may contain the other
     * @param searchSeq The String which may be contained within the other
     * @return If searchSeq is found within seq.
     */
    public static boolean contains(final String seq, final String searchSeq) {
        return StringUtilsWrapper.contains(seq, searchSeq);
    }
}
