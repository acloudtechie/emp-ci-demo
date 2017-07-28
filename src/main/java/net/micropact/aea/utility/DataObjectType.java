package net.micropact.aea.utility;

/**
 * This Enum is for the core entellitrak data types.
 *
 * @author zmiller
 */
public enum DataObjectType implements IJson{

    /**
     * NONE doesn't seem to be used for anything.
     */
    NONE(0, "None", "None"),
    /**
     * Tracked Data Object.
     */
    TRACKING(1, "Tracking", "Tracked Data Object"),
    /**
     * Reference Data Object.
     */
    REFERENCE(2, "Reference", "Reference Data Object"),
    /**
     * EScan Data Object.
     */
    ESCAN(3, "Scan", "Scan Data Object");

    private final int etkNumber;
    private final String display;
    private final String espValue;

    /**
     * Constructor.
     *
     * @param entellitrakNumber The number which core entellitrak uses to refer to this type of element in the database
     * @param theDisplay A user-friendly display String
     * @param theEspValue the value that ESP uses to refer to this data object type
     */
    DataObjectType(final int entellitrakNumber, final String theDisplay, final String theEspValue){
        etkNumber = entellitrakNumber;
        display = theDisplay;
        espValue = theEspValue;
    }

    /**
     * Get the number that core entellitrak uses to refer t othis data object type.
     *
     * @return The number that core entellitrak uses to refer to this data object type.
     */
    public int getEntellitrakNumber(){
        return etkNumber;
    }

    /**
     * Get the user-friendly display String.
     *
     * @return A user-friendly display String.
     */
    public String getDisplay(){
        return display;
    }

    /**
     * Gets the value that the ESP tool uses to reference this data object within the spreadsheet.
     *
     * @return the ESP Value
     */
    public String getEspValue(){
        return espValue;
    }

    /**
     * This method converts the core entellitrak number for a data object type into an enum.
     *
     * @param entellitrakNumber A number which entellitrak uses to identify a data object type.
     * @return {@link DataObjectType} representing the given entellitrak id.
     */
    public static DataObjectType getDataObjectType(final long entellitrakNumber){

        for(final DataObjectType type : DataObjectType.values()){
            if(type.getEntellitrakNumber() == entellitrakNumber){
                return type;
            }
        }

        throw new IllegalArgumentException(
                String.format("\"%s\" is not a number used by core entellitrak to represent a data object type.",
                        entellitrakNumber));
    }

    @Override
    public String encode() {
        return JsonUtilities.encode(Utility.arrayToMap(String.class, Object.class, new Object[][]{
            {"display", getDisplay()}
        }));
    }
}
