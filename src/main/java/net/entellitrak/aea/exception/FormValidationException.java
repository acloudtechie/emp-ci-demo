package net.entellitrak.aea.exception;

import com.entellitrak.ApplicationException;

/**
 * An exception that is thrown when validating a form.
 *
 * @author alee
 */
public class FormValidationException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    /**
     * A form validation exception.
     *
     * @param msg A description of what went wrong.
     */
    public FormValidationException(final String msg) {
        super(msg);
    }

    /**
     * A form validation exception.
     *
     * @param msg A description of what went wrong.
     * @param exception The underlying {@link Exception}
     */
    public FormValidationException(final String msg, final Exception exception) {
        super(msg, exception);
    }

    /**
     * A form validation exception.
     *
     * @param exception The underlying {@link Exception}
     */
    public FormValidationException(final Exception exception) {
        super(exception);
    }

}