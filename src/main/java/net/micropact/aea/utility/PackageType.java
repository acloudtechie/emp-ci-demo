package net.micropact.aea.utility;

/**
 * Defines the types of packages a script object can be associated with.
 *
 * @author aclee
 *
 */
public enum PackageType {

    /** Standard. */
    STANDARD(1),
    /** Data Type Plugin. */
    DATA_TYPE_PLUGIN(2);

    private final int packageTypeNumber;

    /**
     * Default Constructor.
     *
     * @param thePackageTypeNumber The number which core entellitrak uses to refer to this type script package.
     */
    PackageType (final int thePackageTypeNumber){
        packageTypeNumber = thePackageTypeNumber;
    }

    /**
     * Returns the number that core entellitrak uses to refer to this script package type.
     *
     * @return The number that core entellitrak uses to refer to this script package type.
     */
    public int getPackageTypeNumber(){
        return packageTypeNumber;
    }

    /**
     * This method converts the core entellitrak number for a script package type into an enum.
     *
     * @param packageTypeNumber A number which entellitrak uses to identify a script package type.
     * @return {@link PackageType} representing the given packageTypeNumber.
     */
    public static PackageType getPackageType(final Number packageTypeNumber){

        if (packageTypeNumber == null) {
            return null;
        }

        for(final PackageType type : PackageType.values()) {
            if(type.getPackageTypeNumber() == packageTypeNumber.intValue()){
                return type;
            }
        }

        throw new IllegalArgumentException(
                String.format("\"%s\" is not a number used by core entellitrak to represent a script package type.",
                        packageTypeNumber));
    }
}
