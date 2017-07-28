package net.micropact.aea.core.wrappedAPIs;

import org.apache.commons.lang.SystemUtils;

/**
 * Wrapper around {@link SystemUtils}.
 *
 * @author Zachary.Miller
 */
public final class SystemUtilsWrapper {

    /**
     * Utility classes do not need public constructors.
     */
    private SystemUtilsWrapper(){}

    /**
     * Wrapper around {@link SystemUtils}.LINE_SEPARATOR.
     *
     * @return The System Line separator.
     */
    public static String getLineSeparator(){
        return SystemUtils.LINE_SEPARATOR;
    }
}
