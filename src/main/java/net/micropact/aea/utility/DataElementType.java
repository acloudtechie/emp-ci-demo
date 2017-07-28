package net.micropact.aea.utility;

/**
 * This Enum is for the core entellitrak data types.
 *
 * @author zmiller
 */
public enum DataElementType {

    /**
     * Text.
     */
    TEXT(1, "Text"),
    /**
     * Number.
     */
    NUMBER(2, "Number"),
    /**
     * Date.
     */
    DATE(3, "Date"),
    /**
     * Currency.
     */
    CURRENCY(4, "Currency"),
    /**
     * Yes/No.
     */
    YES_NO(5, "Yes/No"),
    /**
     * File (includes regular, DM and eScan).
     */
    FILE(8, "File"),
    /**
     * State (this is the built-in state field for BTOs).
     */
    STATE(9, "State"),
    /**
     * Passsword.
     */
    PASSWORD(10, "Password"),
    /**
     * Long Text.
     */
    LONG_TEXT(11, "Long Text"),
    /**
     * Timestamp.
     */
    TIMESTAMP(12, "Timestamp"),
    /**
     * Core uses None to refer to plugins.
     */
    NONE(13, "Plug-in"),
    /**
     * I have not seen this used in entellitrak.
     */
    LONG(14, "Long");

    private final int etkNumber;
    private final String espIdentifier;

    /**
     * Constructor.
     *
     * @param entellitrakNumber The number which core entellitrak uses to refer to this type of element in the database
     * @param theEspIdentifier The String ESP uses to refer to this data element type.
     */
    DataElementType(final int entellitrakNumber, final String theEspIdentifier){
        etkNumber = entellitrakNumber;
        espIdentifier = theEspIdentifier;
    }

    /**
     * Get the number that core entellitrak uses to refer to this data element.
     *
     * @return The number that core entellitrak uses to refer to this data element
     */
    public int getEntellitrakNumber(){
        return etkNumber;
    }

    public String getEspIdentifier(){
        return espIdentifier;
    }

    /**
     * This method converts the core entellitrak number for a data element type into an enum.
     *
     * @param entellitrakNumber A number which entellitrak uses to identify a data element type.
     * @return {@link DataElementType} representing the given entellitrak id.
     */
    public static DataElementType getDataElementType(final long entellitrakNumber){

        for(final DataElementType type : DataElementType.values()){
            if(type.getEntellitrakNumber() == entellitrakNumber){
                return type;
            }
        }

        throw new IllegalArgumentException(
                String.format("\"%s\" is not a number used by core entellitrak to represent a data type.",
                        entellitrakNumber));
    }
}
