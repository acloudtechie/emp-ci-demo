package net.micropact.aea.utility;

/**
 * Enum containing the Script Object Handler Types that entellitrak uses.
 * The values can be found in com.micropact.entellitrak.workspace.model.HandlerType
 *
 * @author zmiller
 */
public enum ScriptObjectHandlerType {

    NONE(0, "None"),
    ADV_SEARCH_PROCESSOR(1, "AdvancedSearchEventHandler"),
    DISPLAY_MAPPING_HANDLER(2, "DisplayMappingHandler"),
    FORM_ELEMENT_EVENT_HANDLER(3, "FormElementEventHandler"),
    FORM_EVENT_HANDLER(4, "FormEventHandler"),
    JOB_HANDLER(5, "JobHandler"),
    LOOKUP_HANDLER(6, "LookupHandler"),
    PAGE_CONTROLLER(7, "PageController"),
    TRANSITION_HANDLER(8, "TransitionHandler"),
    USER_EVENT_HANDLER(9, "UserEventHandler"),
    SCAN_EVENT_HANDLER(10, "ScanEventHandler"),
    STEP_BASED_PAGE(11, "StepBasedPageHandler"),
    FORM_EXECUTION_HANDLER(12, "FormExecutionHandler"),
    DATA_OBJECT_EVENT_HANDLER(13, "DataObjectEventHandler"),
    APPLY_CHANGES_EVENT_HANDLER(14, "ApplyChangesEventHandler"),
    CHANGE_HANDLER(15, "ChangeHandler"),
    CLICK_HANDLER(16, "ClickHandler"),
    NEW_HANDLER(17, "NewHandler"),
    READ_HANDLER(18, "ReadHandler"),
    SAVE_HANDLER(19, "SaveHandler"),
    OFFLINE_SYNC_HANDLER(20, "OfflineSyncHandler"),
    REFERENCE_OBJECT_EVENT_HANDLER(21, "ReferenceObjectEventHandler"),
    ENDPOINT_HANDLER(22, "EndpointHandler"),
    DEPLOYMENT_HANDLER(23, "DeploymentHandler"),
    ELEMENT_FILTER_HANDLER(24, "ElementFilterHandler"),
    RECORD_FILTER_HANDLER(25, "RecordFilterHandler");


    /**
     * The id which entellitrak internally uses to represent the Script Object Handler.
     */
    private final int id;
    /**
     * The name entellitrak uses to represent the Script Object Handler.
     */
    private final String name;

    /**
     * Constructor.
     *
     * @param scriptObjectHandlerTypeId The id which entellitrak internally uses to represent the
     *  Script Object Handler.
     * @param scriptObjectHandlerTypeName The name entellitrak uses to represent the Script Object Handler.
     */
    ScriptObjectHandlerType(final int scriptObjectHandlerTypeId, final String scriptObjectHandlerTypeName){
        id = scriptObjectHandlerTypeId;
        name = scriptObjectHandlerTypeName;
    }

    /**
     * Get the number entellitrak uses internally to represent the Script Object Handler.
     *
     * @return The id entellitrak internally uses to represent the Script Object Handler.
     */
    public int getId(){
        return id;
    }

    /**
     * Get the name entellitrak uses to represent the Script Object Handler.
     *
     * @return The name entellitrak uses to represent the Script Object Handler.
     */
    public String getName(){
        return name;
    }

    /**
     * Returns the ScriptObjectHandler given the id that entellitrak uses to reference it.
     *
     * @param id The id entellitrak internally uses to represent the Script Object Handler.
     * @return The ScriptObjectHandlerType corresponding to the given internal entellitrak id.
     */
    public static ScriptObjectHandlerType getById(final int id){
        for(final ScriptObjectHandlerType handlerType : values()){
            if(id == handlerType.getId()){
                return handlerType;
            }
        }
        throw new IllegalArgumentException(String.format("Could not find ScriptObjectHandlerType for id: %s", id));
    }

    /**
     * Returns the ScriptObjectHandler given the id that entellitrak uses to reference it.
     *
     * @param id The id entellitrak internally uses to represent the Script Object Handler.
     * @return The ScriptObjectHandlerType corresponding to the given internal entellitrak id.
     */
    public static ScriptObjectHandlerType getById(final Number id){
        if (id == null) {
            return null;
        }

        for(final ScriptObjectHandlerType handlerType : values()){
            if(id.intValue() == handlerType.getId()){
                return handlerType;
            }
        }
        throw new IllegalArgumentException(String.format("Could not find ScriptObjectHandlerType for id: %s", id));
    }
}
