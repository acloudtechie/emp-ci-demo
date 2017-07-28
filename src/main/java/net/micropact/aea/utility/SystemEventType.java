package net.micropact.aea.utility;

/**
 * An Enum of the possible System Event Types in entellitrak. These are the Event Types that entellitrak allows
 * you to add listeners to.
 *
 * Values can be found in com.micropact.entellitrak.system.events.SystemEventType
 *
 * @author zmiller
 */
public enum SystemEventType {
    /**
     * User Profile Created.
     */
    USER_CREATE(1),
    /**
     * User Profile Updated.
     */
    USER_UPDATE(2),
    /**
     * User Profile Deleted.
     */
    USER_DELETE(3),
    /**
     * Advanced Search (this does not just run when the search is run, it also runs for instance when the user
     * goes to edit searches).
     */
    ADVANCED_SEARCH(4),
    /**
     * Apply Changes complete.
     */
    APPLY_CHANGES_COMPLETE(5),
    /**
     * Deployment Handler.
     */
    NEW_DEPLOYMENT(6);

    /**
     * The id which entellitrak internally uses to represent the Script Object Language.
     */
    private final int id;


    /**
     * Constructor.
     *
     * @param systemEventTypeId The id entellitrak uses internally to refer to the System Event Type.
     */
    SystemEventType(final int systemEventTypeId){
        id = systemEventTypeId;
    }

    /**
     * Get the id entellitrak internally uses to represent the System Event Type.
     *
     * @return The id entellitrak internally uses to represent the System Event Type
     */
    public int getId(){
        return id;
    }
}
