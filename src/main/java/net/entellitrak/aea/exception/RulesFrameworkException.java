package net.entellitrak.aea.exception;

/**
 * This class is used for exceptions related to the AE Architecture Rules Framework.
 *
 * @author zmiller
 */
public class RulesFrameworkException extends AeaException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a Rules Framework Exception with a descriptive message and underlying exception.
     *
     * @param msg Description of what went wrong
     * @param e The underlying {@link Exception}
     */
    public RulesFrameworkException(final String msg, final Exception e){
        super(msg, e);
    }

    /**
     * Creates a Rules Framework Exception with a descriptive message of what went wrong.
     *
     * @param msg Description of what went wrong
     */
    public RulesFrameworkException(final String msg){
        super(msg);
    }

    /**
     * Creates a Rules Framework Exception from an underlying exception.
     *
     * @param e The underlying {@link Exception}
     */
    public RulesFrameworkException(final Exception e){
        super(e);
    }
}
