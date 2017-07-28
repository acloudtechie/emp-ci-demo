package net.micropact.aea.core.enums;

/**
 * This enum represents the statuses that Apply Changes can have in the update log.
 * The entellitrak number is the identifier that entellitrak uses to refer to the status, however note that although we
 * are using longs to represent the numbers, core actually uses String representations for the numbers.
 *
 * @author zmiller
 */
public enum UpdateLogStatus {

    /* Please keep the values sorted.
     * The values come from com.micropact.entellitrak.cfg.update.UpdateStatus*/

    /* POSITIVE VALUES */
    // 100
    STARTING_UPDATE(100),
    UPDATING_APPLIED_CHANGES(125),
    GETTING_EXISTING_WORKING_COPY_ARCHIVE(130),
    CREATING_WORKING_COPY_ARCHIVE(140),
    REFRESH_ETDL(145),
    REFRESH_LAST_DEPLOYED_ARCHIVE(175),
    // 200
    UPDATING_DATABASE(200),
    DEPLOYING_WORKFLOW_PROCESS(250),
    INITIALIZING_PROCESS_DEPLOYER(255),
    GENERATING_NEW_GRAPH(260),
    SAVING_CFG_GRAPH(270),
    // 300
    UPDATING_TRACKING_CONFIG(300),
    UPDATING_APPLIED_CHANGES_OF_CFG_GRAPH(325),
    REFRESH_REPOINT_ARCHIVE(340),
    SAVING_ARCHIVE(350),
    DELETING_PREVIOUS_CONFIG(375),
    // 400
    RELOADING_TRACKING_CONFIG(400),
    UPDATE_THREAD_COMPLETED_SUCCESSFULLY(450),
    // 500
    UPDATE_MANAGER_READY(500),

    /* NEGATIVE VALUES */
    // -100
    ERROR_STARTING_UPDATE(-100),
    ERROR_UPDATING_APPLIED_CHANGES(-125),
    ERROR_ARCHIVING_WORKING_COPY(-150),
    // -200
    ERROR_UPDATING_DATABASE(-200),
    ERROR_DEPLOYING_WORKFLOW_PROCESS(-250),
    ERROR_GENERATING_NEW_GRAPH(-260),
    ERROR_SAVING_CFG_GRAPH(-270),
    // -300
    ERROR_UPDATING_TRACKING_CONFIG(-300),
    ERROR_UPDATING_APPLIED_CHANGES_OF_CFG_GRAPH(-325),
    ERROR_SAVING_ARCHIVE(-350),
    // -400
    ERROR_RELOADING_TRACKING_CONFIG(-400),
    UPDATE_THREAD_ERROR(-450),
    // -500
    ERROR_UPDATE_MANAGER_RESET(-500);


    private final long etkNumber;

    /**
     * Simple Constructor.
     *
     * @param theEntellitrakId the identifier that entellitrak uses to refer to the status
     */
    UpdateLogStatus(final long theEntellitrakId){
        etkNumber = theEntellitrakId;
    }

    /**
     * Get the id that entellitrak uses internally to refer to this Log Status.
     *
     * @return The number that core entellitrak uses to refer to Update Log Status
     */
    public long getEntellitrakNumber(){
        return etkNumber;
    }

    /**
     * This method converts the core entellitrak number for an update log status to an enum.
     *
     * @param entellitrakNumber A number which entellitrak uses to identify a status.
     * @return {@link UpdateLogStatus} representing the given entellitrak number.
     */
    public static UpdateLogStatus getDataUpdateLogStatus(final long entellitrakNumber){

        for(final UpdateLogStatus logStatus : UpdateLogStatus.values()){
            if(logStatus.getEntellitrakNumber() == entellitrakNumber){
                return logStatus;
            }
        }

        throw new IllegalArgumentException(
                String.format("\"%s\" is not a number used by core entellitrak to represent an Update Log Status.",
                        entellitrakNumber));
    }

    /**
     * This method converts a String which core stores in the ETK_UPDATE_LOG table to a human-readable String.
     * The reason why this has to be done in a static method is because core stores ridiculous things like
     * &quot;--125, 500&quot; and if we come across one of these cases we will just return the string itself.
     *
     * @param updateLogStatus The log entry string which core stores
     * @return A formatted display of the log status String
     */
    public static String getFormattedString(final String updateLogStatus){
        try{
            return UpdateLogStatus.getDataUpdateLogStatus(Long.parseLong(updateLogStatus)).toString();
        }catch(final Exception e){
            return updateLogStatus;
        }
    }
}
