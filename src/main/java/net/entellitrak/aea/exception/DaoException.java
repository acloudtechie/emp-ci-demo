package net.entellitrak.aea.exception;

/**
 * Defines an AEA DAO exception.
 *
 * @author aclee
 *
 */
public class DaoException extends Exception {

	private static final long serialVersionUID = 7331516093118499531L;

	/**
	 * Exception constructor.
	 *
	 * @param errorMessage Message explaining the error.
	 */
	public DaoException(final String errorMessage) {
		super(errorMessage);
	}

	/**
	 * Exception constructor.
	 *
	 * @param errorMessage Message explaining the error.
	 * @param t The exception.
	 */
	public DaoException(final String errorMessage, final Throwable t) {
		super(errorMessage, t);
	}

	/**
	 * Exception constructor.
	 *
	 * @param t The exception.
	 */
	public DaoException(final Throwable t) {
		super(t);
	}
}
