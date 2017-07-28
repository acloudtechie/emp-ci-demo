package net.micropact.aea.utility;

/**
 * This enum represents the display formats that entellitrak uses for Lookups of type System Object.
 *
 * @author zachary.miller
 */
public enum SystemObjectDisplayFormat {
    /**
     * Display just the username (jdoe).
     */
    ACCOUNT_NAME(1, "Account Name"),
    /**
     * Display LastName, FirstName, MI (Doe, John C).
     */
    LASTNAME_FIRSTNAME_MI(2, "LastName, FirstName MI");

    private final int id;
    private final String name;

    /**
     * Constructor.
     *
     * @param anId the id entellitrak uses internally to refer to this display format.
     * @param aName A user-friendly representation of the display format.
     */
    SystemObjectDisplayFormat(final int anId, final String aName){
        id = anId;
        name = aName;
    }

    public int getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    /**
     * Translates the number entellitrak uses internally to refer to a display format to a
     * {@link SystemObjectDisplayFormat}.
     *
     * @param id id entellitrak uses to refer to this display format.
     * @return the {@link SystemObjectDisplayFormat}
     */
    public static SystemObjectDisplayFormat getById(final int id){
        for(final SystemObjectDisplayFormat anObject : values()){
            if(id == anObject.getId()){
                return anObject;
            }
        }

        throw new IllegalArgumentException(String.format("Could not find SystemObjectDisplayFormat for id: %s", id));
    }
}