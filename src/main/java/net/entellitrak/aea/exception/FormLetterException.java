package net.entellitrak.aea.exception;

/**
 * This class is for exceptions related to the AE Architecture Form Letter Generator.
 *
 * @author zmiller
 */
public class FormLetterException extends AeaException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a Form Letter Exception with a descriptive message.
     *
     * @param msg Description of what went wrong
     */
    public FormLetterException(final String msg) {
        super(msg);
    }

    /**
     * Creates a Form Letter Exception from an underlying exception and a descriptive message of what went wrong.
     *
     * @param msg Description of what went wrong
     * @param exception The underlying {@link Exception}
     */
    public FormLetterException(final String msg, final Exception exception){
        super(msg, exception);
    }
}
