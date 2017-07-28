package net.entellitrak.aea.exception;

import com.entellitrak.ApplicationException;

/**
 * <p>This class is for exceptions related to AE Architecture components.</p>
 * <p>
 *  It extends ApplicationException so that it can be, but does not have to be caught when used
 *  from core entellitrak handlers.
 * </p>
 *
 * @author zmiller
 */
public class AeaException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an Exception with a specified message.
     *
     * @param msg A description of what went wrong
     */
    public AeaException(final String msg) {
        super(msg);
    }

    /**
     * Creates an Exception with both a message and an underlying exception cause.
     *
     * @param msg A description of what went wrong
     * @param exception The underlying {@link Exception}
     */
    public AeaException(final String msg, final Exception exception) {
        super(msg, exception);
    }

    /**
     * Create an exception with an underlying Exception cause.
     *
     * @param exception The underlying {@link Exception}
     */
    public AeaException(final Exception exception) {
        super(exception);
    }

}
