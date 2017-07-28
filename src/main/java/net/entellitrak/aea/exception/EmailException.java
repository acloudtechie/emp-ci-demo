package net.entellitrak.aea.exception;

/**
 * This class is for exceptions related to the AE Architecture Email Utility.
 *
 * @author zmiller
 */
public class EmailException extends AeaException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an Email Exception with a particular message indicating what went wrong.
     *
     * @param msg Description of what went wrong
     */
    public EmailException(final String msg){
        super(msg);
    }

    /**
     * Creates an Email Exception with a message indicating what went wrong and the underlying exception which caused
     * the issue.
     *
     * @param msg Description of what went wrong
     * @param e The underlying {@link Exception}
     */
    public EmailException(final String msg, final Exception e){
        super(msg, e);
    }

    /**
     * Creates an Email Exception with an underlying Exception indicating what went wrong.
     *
     * @param e The underlying {@link Exception}
     */
    public EmailException(final Exception e){
        super(e);
    }
}
