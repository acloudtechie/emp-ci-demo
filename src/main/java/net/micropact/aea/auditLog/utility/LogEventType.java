package net.micropact.aea.auditLog.utility;

/**
 * Log event type enumeration.
 *
 * @author MicroPact
 *
 */
public enum LogEventType {

	/**
	 * Create event type.
	 */
	CREATE("Create"),

	/**
	 * Read event type.
	 */
	READ("Read"),

	/**
	 * Update Event Type.
	 */
	UPDATE("Update"),

	/**
	 * Delete Event type.
	 */
	DELETE("Delete");

	private final String eventType;

	/**
	 * Default constructor.
	 *
	 * @param aLogEventType an event type.
	 */
	LogEventType (final String aLogEventType) {
		this.eventType = aLogEventType;
	}

	public String getEventType() {
		return eventType;
	}

	@Override
	public String toString() {
		return eventType;
	}
}
