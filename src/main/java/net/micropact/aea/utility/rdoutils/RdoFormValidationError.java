package net.micropact.aea.utility.rdoutils;

/**
 * This class defines an error with the user input on a form.
 *
 * @author aclee
 *
 */
public class RdoFormValidationError {
	String elementKey;
	String errorMessage;

	public String getElementKey() {
		return elementKey;
	}
	public void setElementKey(final String elementKey) {
		this.elementKey = elementKey;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(final String errorMessage) {
		this.errorMessage = errorMessage;
	}
}
