package net.micropact.aea.utility;

/**
 * enum representing the types of lookups in an entellitrak system.
 *
 * @author zmiller
 */
public enum LookupSourceType {

    /**
     * Data Object.
     */
    DATA_OBJECT_LOOKUP(1, "Data Object"),
    /**
     * SQL.
     */
    QUERY_LOOKUP(2, "SQL Query"),
    /**
     * I have never seen this used by entellitrak.
     */
    PLUGIN_LOOKUP(3, "Plugin"),
    /**
     * Script.
     */
    SCRIPT_LOOKUP(4, "Script"),
    /**
     * System.
     */
    SYSTEM_OBJECT_LOOKUP(5, "System Object");

    private final int etkNumber;
    private final String display;

    /**
     * Constructor.
     *
     * @param entellitrakNumber The number which core entellitrak uses to refer to this type of element in the database
     * @param displayString User-friendly representation of the Lookup Source Type
     */
    LookupSourceType(final int entellitrakNumber, final String displayString){
        etkNumber = entellitrakNumber;
        display = displayString;
    }

    /**
     * Get the number that entellitrak uses internally to refer to this Lookup Source Type.
     *
     * @return The number that core entellitrak uses to refer to this data object type.
     */
    public int getEntellitrakNumber(){
        return etkNumber;
    }

    /**
     * Get the user-friendly display.
     *
     * @return A user-friendly representation of the Lookup Source Type
     */
    public String getDisplay(){
        return display;
    }

    /**
     * This method converts the core entellitrak number for a data object type into an enum.
     *
     * @param entellitrakNumber A number which entellitrak uses to identify a data object type.
     * @return {@link DataObjectType} representing the given entellitrak id.
     */
    public static LookupSourceType getLookupSourceType(final long entellitrakNumber){

        for(final LookupSourceType type : LookupSourceType.values()){
            if(type.getEntellitrakNumber() == entellitrakNumber){
                return type;
            }
        }

        throw new IllegalArgumentException(
                String.format("\"%s\" is not a number used by core entellitrak to represent a lookup type.",
                        entellitrakNumber));
    }
}
