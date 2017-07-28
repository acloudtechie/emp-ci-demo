package net.micropact.aea.utility;

/**
 * This enum represents the possible System Objects usable by Lookups with Lookup Source System Object.
 *
 * @author zmiller
 */
public enum SystemObjectType {
    /**
     * ETK_USER.
     */
    USER(1, "User", "ETK_USER", "USER_ID");

    private final int id;
    private final String name;
    private final String tableName;
    private final String columnName;

    /**
     * Constructor.
     *
     * @param anId number which entellitrak uses internally to represent this System Object Type
     * @param aName User-friendly String to use for this System Object Type
     * @param aTableName The table from which the lookup gets its value
     * @param aColumnName The column from which the lookup gets its value
     */
    SystemObjectType(final int anId, final String aName, final String aTableName, final String aColumnName){
        id = anId;
        name = aName;
        tableName = aTableName;
        columnName = aColumnName;
    }

    /**
     * Get the number which entellitrak uses internally to represent this System Object Type.
     *
     * @return number which entellitrak uses internally to represent this System Object Type
     */
    public int getId(){
        return id;
    }

    /**
     * Get a user-friendly display of the System Object Type.
     *
     * @return User-friendly String to use for this System Object Type
     */
    public String getName(){
        return name;
    }

    /**
     * Translate the number entellitrak uses to refer to a System Object Type to the actual System Object Type.
     *
     * @param id number which entellitrak uses internally to represent this System Object Type
     * @return The System Object Type represented by the id
     */
    public static SystemObjectType getById(final int id){
        for(final SystemObjectType anObject : values()){
            if(id == anObject.getId()){
                return anObject;
            }
        }

        throw new IllegalArgumentException(String.format("Could not find SystemObjectType for id: %s", id));
    }

    /**
     * Get the table name which the lookup gets its value from.
     *
     * @return The table which the lookup gets its value from.
     */
    public String getTableName(){
        return tableName;
    }

    /**
     * Get the column which the lookup gets its value from.
     *
     * @return The column which the lookup gets its value from.
     */
    public String getColumnName(){
        return columnName;
    }
}
