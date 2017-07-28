package net.micropact.aea.auditLog.utility;

/**
 * User or System event type.
 *
 * @author MicroPact
 *
 */
public enum UserSystemEventType {

	/**
	 * User event type.
	 */
	USER("User"),

	/**
	 * System event type.
	 */
	SYSTEM("System");

	private final String eventType;

	/**
	 * Default constructor.
	 *
	 * @param anEventType Event type string.
	 */
	UserSystemEventType (final String anEventType) {
		this.eventType = anEventType;
	}

	public String getEventType() {
		return eventType;
	}

	@Override
	public String toString() {
		return eventType;
	}
}
