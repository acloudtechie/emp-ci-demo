package net.micropact.aea.utility;

/**
 * Enum of the possible types of fields which are supported by entellitrak data forms.
 * Entellitrak actually uses unique names in the database instead of numbers to refer to these fields.
 * This unique name is called the entellitrak name in this class.
 *
 * @author zmiller
 */
public enum FormControlType {

    /**
     * Label.
     */
    LABEL("label"),
    /**
     * Text.
     */
    TEXT("text"),
    /**
     * Textarea.
     */
    TEXTAREA("textarea"),
    /**
     * Select.
     */
    SELECT("select"),
    /**
     * Multiselect.
     */
    MULTI_SELECT("multi-select"),
    /**
     * Radio.
     */
    RADIO("radio"),
    /**
     * Checkbox.
     */
    CHECKBOX("checkbox"),
    /**
     * Button.
     */
    BUTTON("button"),
    /**
     * Hidden.
     */
    HIDDEN("hidden"),
    /**
     * Line.
     */
    LINE("line"),
    /**
     * File.
     */
    FILE("file"),
    /**
     * Password.
     */
    PASSWORD("password"),
    /**
     * Date.
     */
    DATE("date"),
    /**
     * Timestamp.
     */
    TIMESTAMP("timestamp"),
    /**
     * Yes/No.
     */
    YES_NO("yesno"),
    /**
     * Script (Custom Data Types).
     */
    SCRIPT_BASED("custom");

    private String etkName;

    /**
     * Constructor.
     *
     * @param entellitrakName The name that entellitrak uses to refer to this Form Control.
     */
    FormControlType(final String entellitrakName){
        etkName = entellitrakName;
    }

    /**
     * Get the unique name that entellitrak uses to refer to this form control (ETK_FORM_CONTROL.FORM_CONTROL_TYPE).
     *
     * @return The unique name that entellitrak uses to refer to this form control.
     */
    public String getEntellitrakName(){
        return etkName;
    }
}
