package net.entellitrak.aea.exception;

/**
 * This class is used for exceptions which are part of the AE Architecture Template Utility.
 *
 * @author zmiller
 */
public class TemplateException extends AeaException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a Template Exception with a message indicating the problem.
     *
     * @param msg Description of what went wrong
     */
    public TemplateException(final String msg) {
        super(msg);
    }

    /**
     * Creates a Template Exception with a message indicating the problem and an underlying exception.
     *
     * @param msg Description of what went wrong
     * @param exception The underlying {@link Exception}
     */
    public TemplateException(final String msg, final Exception exception){
        super(msg, exception);
    }
}
