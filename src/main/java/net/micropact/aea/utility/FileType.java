package net.micropact.aea.utility;

/**
 * Enum containing the FileTypes that entellitrak uses.
 *
 * @author zmiller
 */
public enum FileType {


    /**
     * This represents regular (non DM files).
     */
    USER_FILE(1),
    /**
     * I have never seen entellitrak use this option.
     */
    TRACKING_CONFIG_FILE(2),
    /**
     * I have never seen entellitrak use this option.
     */
    PROCESS_CONFIG_FILE(3),
    /**
     * This represents Document Management (DM) files.
     */
    DOCUMENT_MANAGEMENT_FILE(4);

    /**
     * The id which entellitrak internally uses to represent the Script Object Handler.
     */
    private final int entellitrakNumber;

    /**
     * Constructor.
     *
     * @param fileTypeEntellitrakNumber The id which entellitrak internally uses to represent the
     *  File Type.
     */
    FileType(final int fileTypeEntellitrakNumber){
        entellitrakNumber = fileTypeEntellitrakNumber;
    }

    /**
     * Get the number that entellitrak uses internalyl to represent the File Type.
     *
     * @return The id entellitrak internally uses to represent the File Type.
     */
    public int getEntellitrakNumber(){
        return entellitrakNumber;
    }

    /**
     * Returns the FileType given the id that entellitrak uses to reference it.
     *
     * @param entellitrakNumber The id entellitrak internally uses to represent the File Type.
     * @return The FileType corresponding to the given internal entellitrak id.
     */
    public static FileType getByEntellitrakNumber(final int entellitrakNumber){
        for(final FileType fileType : values()){
            if(entellitrakNumber == fileType.getEntellitrakNumber()){
                return fileType;
            }
        }
        throw new IllegalArgumentException(String.format("Could not find FileType for entellitrakNumber: %s", entellitrakNumber));
    }
}
