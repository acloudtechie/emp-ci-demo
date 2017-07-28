package net.micropact.aea.core.wrappedAPIs;

import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * Wrapper around {@link ExceptionUtils}.
 *
 * @author Zachary.Miller
 */
public final class ExceptionUtilsWrapper {

    /**
     * Utility classes do not need public constructors.
     */
    private ExceptionUtilsWrapper(){}

    /**
     * Get a full stack trace string for a throwable.
     *
     * @param throwable the throwable to get the stack trace for
     * @return the string representation
     */
    public static String getFullStackTrace(final Throwable throwable){
        return ExceptionUtils.getFullStackTrace(throwable);
    }

    /**
     * Get a partial stack trace string representation for a throwable.
     *
     * @param throwable the throwable to get the stack trace for
     * @return the string representation
     */
    public static String getStackTrace(final Throwable throwable) {
        return ExceptionUtils.getStackTrace(throwable);
    }
}
