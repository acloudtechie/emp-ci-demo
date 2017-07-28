package net.entellitrak.aea.exception;

/**
 * Exception thrown when parsing a PDF document using PDF Template File component.
 *
 * @author aclee
 *
 */
public class PdfParseException extends AeaException {
    /**
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = -6442809167169945738L;

	/**
	 * Constructor.
	 *
     * @param msg Description of what went wrong
     */
    public PdfParseException(final String msg){
        super(msg);
    }

    /**
     * Constructor.
     *
     * @param msg Description of what went wrong
     * @param e The underlying {@link Exception}
     */
    public PdfParseException(final String msg, final Exception e){
        super(msg, e);
    }
}