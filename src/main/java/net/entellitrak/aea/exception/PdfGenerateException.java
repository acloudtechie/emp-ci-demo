package net.entellitrak.aea.exception;

/**
 * Exception thrown when generating a PDF document using PDF Template File
 * component.
 *
 * @author aclee
 *
 */
public class PdfGenerateException extends AeaException {
    /**
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = -6442809167169945738L;

	/**
	 * Constructor.
	 *
     * @param msg Description of what went wrong
     */
    public PdfGenerateException(final String msg){
        super(msg);
    }

    /**
     * Constructor.
     *
     * @param msg Description of what went wrong
     * @param e The underlying {@link Exception}
     */
    public PdfGenerateException(final String msg, final Exception e){
        super(msg, e);
    }
}
