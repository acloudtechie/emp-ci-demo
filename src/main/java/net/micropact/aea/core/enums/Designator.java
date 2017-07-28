package net.micropact.aea.core.enums;

/**
 * This enum represents the different devices that entellitrak supports.
 *
 * @author zmiller
 */
public enum Designator {

    /** All Devices. */
    ALL_DEVICES(1),
    /** Large Phone. */
    LARGE_PHONE(2),
    /** Small Tabled. */
    SMALL_TABLET(3),
    /** Large Tablet. */
    LARGE_TABLET(4),
    /** Desktop. */
    DESKTOP(5);

    private final long etkNumber;

    /**
     * Simple Constructor.
     *
     * @param theEntellitrakId the identifier that entellitrak uses to refer to the designator
     */
    Designator(final long theEntellitrakId){
        etkNumber = theEntellitrakId;
    }

    /**
     * Get the number that core entellitrak uses to refer to the designator.
     *
     * @return The number that core entellitrak uses to refer to the designator
     */
    public long getEntellitrakNumber(){
        return etkNumber;
    }

    /**
     * This method converts the core entellitrak number for a designator to an enum.
     *
     * @param entellitrakNumber A number which entellitrak uses to identify a designator.
     * @return {@link Designator} representing the given entellitrak number.
     */
    public static Designator getDesignator(final long entellitrakNumber){

        for(final Designator designator : Designator.values()){
            if(designator.getEntellitrakNumber() == entellitrakNumber){
                return designator;
            }
        }

        throw new IllegalArgumentException(
                String.format("\"%s\" is not a number used by core entellitrak to represent a Designator.",
                        entellitrakNumber));
    }
}
