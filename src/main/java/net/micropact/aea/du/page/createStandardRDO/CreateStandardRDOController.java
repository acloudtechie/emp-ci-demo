package net.micropact.aea.du.page.createStandardRDO;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.legacy.util.StringUtility;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.enums.Designator;
import net.micropact.aea.utility.DataElementRequiredLevel;
import net.micropact.aea.utility.DataElementType;
import net.micropact.aea.utility.FormControlType;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.LookupSourceType;
import net.micropact.aea.utility.Utility;

/**
 * This controller is for a page which generates a &quot;Standard&quot; Reference Data Object.
 * This is because most Reference Data Objects have columns for Name, Code, Order, Start Date and End Date.
 * The page submits to itself instead of making an AJAX call, so that is why it contains the creation logic.
 *
 * @author zmiller
 */
public class CreateStandardRDOController implements PageController{

    /** Maximum number of characters allowed for table name. */
    private static final int MAX_TABLE_NAME_SIZE = 30;
    /** Default size of entellitrak text fields. */
    private static final int DEFAULT_TEXT_SIZE = 255;
    /** entellitrak's form designer calculates an extra padding of 4
     * when determining the y coordinate of elements.*/
    private static final int FORM_VERTICAL_PADDING = 4;
    /** Default height of entellitrak data form elements. */
    private static final int DEFAULT_FORM_ELEMENT_HEIGHT = 25;
    /** This is the default width we will make text elements. It is NOT the width entellitrak uses by default. */
    private static final int DEFAULT_FORM_ELEMENT_WIDTH = 300;
    /** This is how wide entellitrak date fields are on a form. */
    private static final int DEFAULT_FORM_DATE_WIDTH = 194;

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
            /* requestedAction will say why they are submitting the form. If it is "generate" it means that they
             * want the Data Object created. */
            final String requestedAction = etk.getParameters().getSingle("requestedAction");

            // Get all the post parameters
            final String tableName = etk.getParameters().getSingle("tableName");
            final String name = etk.getParameters().getSingle("name");
            final String objectName = etk.getParameters().getSingle("objectName");
            //businessKeySegment is the part of a business key that follows "object."
            final String businessKeySegment = etk.getParameters().getSingle("businessKeySegment");
            final String label = etk.getParameters().getSingle("label");
            final String description = etk.getParameters().getSingle("description");
            final boolean generateLookup = "1".equals(etk.getParameters().getSingle("generateLookup"));
            final String lookupName = etk.getParameters().getSingle("lookupName");

            final TextResponse response = etk.createTextResponse();

            final List<Map<String, Object>> dataElements = getDefaultDataElements();

            Long dataObjectId = null;
            Long dataFormId = null;
            Long dataViewId = null;
            Long lookupDefinitionId = null;
            String error = null;

            if("generate".equals(requestedAction)){
                // Declare a bunch of other variables mainly related to Forms and Views which will be used later
                final String businessKey = String.format("object.%s", businessKeySegment);

                final String formTitle = name;
                final String formName = String.format("%s Default Form", name);
                final String formBusinessKey = String.format("%s.form.%sDefaultForm", businessKey, businessKeySegment);
                final String formDescription = String.format("Default form for %s", name);

                final String viewName = String.format("%s Default View", name);
                final String viewTitle = String.format("%s Listing", name);
                final String viewBusinessKey = String.format("%s.view.%sDefaultView", businessKey, businessKeySegment);
                final String viewDescription = String.format("%s Listing", name);

                // This is where we will display any validation errors
                if(StringUtility.isBlank(name)){
                    /* We check that the name's not blank. */
                    error = "Name cannot be blank.";
                }else if(tableName.length() > MAX_TABLE_NAME_SIZE){
                    error = String.format("The requested table name is %s characters, but the limit is %s characters",
                            tableName.length(),
                            MAX_TABLE_NAME_SIZE);
                }else if(0 < etk.createSQL("SELECT COUNT(*) FROM etk_data_object WHERE business_key = :businessKey AND tracking_config_id = :trackingConfigId")
                        .setParameter("businessKey", businessKey)
                        .setParameter("trackingConfigId", Utility.getTrackingConfigIdNext(etk))
                        .fetchInt()){
                    // Check that the business key is not being used
                    error = String.format("The Object Business Key \"%s\" was already in use.", businessKey);
                }else if(0 < etk.createSQL("SELECT COUNT(*) FROM etk_data_object WHERE table_name = :tableName AND tracking_config_id = :trackingConfigId")
                        .setParameter("tableName", tableName)
                        .setParameter("trackingConfigId", Utility.getTrackingConfigIdNext(etk))
                        .fetchInt()){
                    // Check that the table is not being used.
                    error = String.format("The Table Name \"%s\" was already in use.", tableName);
                }else if(!name.matches("^[a-zA-Z][a-zA-Z\\d ]*$")){
                    // Note the above check is stricter than it actually HAS to be
                    error = "The chosen name is invalid. Name can only contain alphanumeric characters (and spaces) and cannot start with a number";
                }else{
                    /* ETK_DATA_OBJECT */
                    if(Utility.isSqlServer(etk)){
                        dataObjectId = ((Number) etk.createSQL("INSERT INTO etk_data_object(tracking_config_id, parent_object_id, base_object, table_name, object_type, applied_changes, list_order, list_style, searchable, label, cardinality, object_name, separate_inbox, business_key, name, description, document_management_enabled, designator, auto_assignment) VALUES((SELECT tracking_config_id FROM etk_tracking_config WHERE config_version = (SELECT MAX(config_version) FROM etk_tracking_config)), NULL, 1, :tableName, 2, 0, 0, 1, 1, :label, -1, :objectName, 0, :businessKey, :name, :description, 0, :designator, 1) ")
                        .setParameter("tableName", tableName)
                        .setParameter("name", name)
                        .setParameter("objectName", objectName)
                        .setParameter("businessKey", businessKey)
                        .setParameter("label", label)
                        .setParameter("description", description)
                        .setParameter("designator", Designator.DESKTOP.getEntellitrakNumber())
                        .executeForKey("DATA_OBJECT_ID")).longValue();
                    }else{
                        dataObjectId = getNextHibernateSequenceId(etk);

                        etk.createSQL("INSERT INTO etk_data_object(data_object_id, tracking_config_id, parent_object_id, base_object, table_name, object_type, applied_changes, list_order, list_style, searchable, label, cardinality, object_name, separate_inbox, business_key, name, description, document_management_enabled, designator, auto_assignment) VALUES(:dataObjectId, (SELECT tracking_config_id FROM etk_tracking_config WHERE config_version = (SELECT MAX(config_version) FROM etk_tracking_config)), NULL, 1, :tableName, 2, 0, 0, 1, 1, :label, -1, :objectName, 0, :businessKey, :name, :description, 0, :designator, 1) ")
                        .setParameter("dataObjectId", dataObjectId)
                        .setParameter("tableName", tableName)
                        .setParameter("name", name)
                        .setParameter("objectName", objectName)
                        .setParameter("businessKey", businessKey)
                        .setParameter("label", label)
                        .setParameter("description", description)
                        .setParameter("designator", Designator.DESKTOP.getEntellitrakNumber())
                        .execute();
                    }

                    /* ETK_DATA_ELEMENT */
                    for(final Map<String, Object> dataElement : dataElements){

                        final long dataElementId;

                        if(Utility.isSqlServer(etk)){
                            dataElementId = etk.createSQL("INSERT INTO etk_data_element(data_object_id, name, data_type, required, validation_required, column_name, primary_key, system_field, index_type, default_value, searchable, is_unique, data_size, bound_to_lookup, lookup_definition_id, element_name, default_to_today, future_dates_allowed, identifier, logged, plugin_registration_id, applied_changes, table_name, business_key, description, stored_in_document_management, used_for_escan) VALUES (:dataObjectId, :name, :dataType, :required, null, :columnName, null, 0, null, null, 1, 0, :dataSize, 0, null, :elementName, 0, :futureDatesAllowed, 0, 0, null, 0, null, :businessKey, :description, 0, 0)")
                                    .setParameter("dataObjectId", dataObjectId)
                                    .setParameter("name", dataElement.get("name"))
                                    .setParameter("businessKey", String.format("%s.element.%s", businessKey, dataElement.get("elementName")))
                                    .setParameter("columnName", dataElement.get("columnName"))
                                    .setParameter("dataType",
                                            ((DataElementType) dataElement.get("dataElementType"))
                                            .getEntellitrakNumber())
                                    .setParameter("elementName", dataElement.get("elementName"))
                                    .setParameter("dataSize", dataElement.get("dataSize"))
                                    .setParameter("futureDatesAllowed",
                                            (Boolean) dataElement.get("futureDatesAllowed") ? 1 : 0)
                                    .setParameter("required",
                                            ((DataElementRequiredLevel) dataElement.get("required"))
                                            .getEntellitrakNumber())
                                    .setParameter("description", dataElement.get("description"))
                                    .executeForKey("DATA_ELEMENT_ID");
                        }else{
                            dataElementId = getNextHibernateSequenceId(etk);

                            etk.createSQL("INSERT INTO etk_data_element(data_element_id, data_object_id, name, data_type, required, validation_required, column_name, primary_key, system_field, index_type, default_value, searchable, is_unique, data_size, bound_to_lookup, lookup_definition_id, element_name, default_to_today, future_dates_allowed, identifier, logged, plugin_registration_id, applied_changes, table_name, business_key, description, stored_in_document_management, used_for_escan) VALUES (:dataElementId, :dataObjectId, :name, :dataType, :required, null, :columnName, null, 0, null, null, 1, 0, :dataSize, 0, null, :elementName, 0, :futureDatesAllowed, 0, 0, null, 0, null, :businessKey, :description, 0, 0)")
                            .setParameter("dataObjectId", dataObjectId)
                            .setParameter("dataElementId", dataElementId)
                            .setParameter("name", dataElement.get("name"))
                            .setParameter("businessKey", String.format("%s.element.%s", businessKey, dataElement.get("elementName")))
                            .setParameter("columnName", dataElement.get("columnName"))
                            .setParameter("dataType",
                                    ((DataElementType) dataElement.get("dataElementType")).getEntellitrakNumber())
                            .setParameter("elementName", dataElement.get("elementName"))
                            .setParameter("dataSize", dataElement.get("dataSize"))
                            .setParameter("futureDatesAllowed", (Boolean) dataElement.get("futureDatesAllowed") ? 1 : 0)
                            .setParameter("required",
                                    ((DataElementRequiredLevel) dataElement.get("required")).getEntellitrakNumber())
                            .setParameter("description", dataElement.get("description"))
                            .execute();
                        }

                        dataElement.put("dataElementId", dataElementId);
                    }

                    /* ETK_DATA_VIEW */
                    if(Utility.isSqlServer(etk)){
                        dataViewId = ((Number) etk.createSQL("INSERT INTO etk_data_view(data_object_id, title, text, default_view, search_view, business_key, name, description) VALUES(:dataObjectId, :title, NULL, 1, 1, :businessKey, :name, :description)")
                                .setParameter("dataObjectId", dataObjectId)
                                .setParameter("name", viewName)
                                .setParameter("title", viewTitle)
                                .setParameter("businessKey", viewBusinessKey)
                                .setParameter("description", viewDescription)
                                .executeForKey("DATA_VIEW_ID")).longValue();
                    }else{
                        dataViewId = getNextHibernateSequenceId(etk);

                        etk.createSQL("INSERT INTO etk_data_view(data_view_id, data_object_id, title, text, default_view, search_view, business_key, name, description) VALUES(:dataViewId, :dataObjectId, :title, NULL, 1, 1, :businessKey, :name, :description)")
                        .setParameter("dataViewId", dataViewId)
                        .setParameter("dataObjectId", dataObjectId)
                        .setParameter("name", viewName)
                        .setParameter("title", viewTitle)
                        .setParameter("businessKey", viewBusinessKey)
                        .setParameter("description", viewDescription)
                        .execute();
                    }

                    /* ETK_DATA_VIEW_ELEMENT */
                    for(int displayOrder = 0; displayOrder < dataElements.size(); displayOrder++){

                        final Map<String, Object> dataElement = dataElements.get(displayOrder);

                        etk.createSQL(Utility.isSqlServer(etk) ? "INSERT INTO etk_data_view_element(data_view_id, name, data_element_id, display_order, label, display_size, multi_value_delimiter, business_key, description, responsiveness_factor) VALUES(:dataViewId, :name, :dataElementId, :displayOrder, :label, null, null, :businessKey, :description, 1)"
                                : "INSERT INTO etk_data_view_element(data_view_element_id, data_view_id, name, data_element_id, display_order, label, display_size, multi_value_delimiter, business_key, description, responsiveness_factor) VALUES(HIBERNATE_SEQUENCE.NEXTVAL, :dataViewId, :name, :dataElementId, :displayOrder, :label, null, null, :businessKey, :description, 1)")
                            .setParameter("dataElementId", dataElement.get("dataElementId"))
                            .setParameter("dataViewId", dataViewId)
                            .setParameter("name", dataElement.get("elementName"))
                            .setParameter("businessKey", String.format("%s.element.%s", viewBusinessKey, dataElement.get("elementName")))
                            .setParameter("label", dataElement.get("name"))
                            .setParameter("description", String.format("%s column.", dataElement.get("name")))
                            .setParameter("displayOrder", displayOrder)
                            .execute();

                    }

                    /* ETK_DATA_FORM */
                    if(Utility.isSqlServer(etk)){
                        dataFormId = ((Number) etk.createSQL("INSERT INTO etk_data_form(data_object_id, title, instructions, default_form, enable_spell_checker, search_form, layout_type, business_key, name, description, offline_form) VALUES(:dataObjectId, :title, NULL, 1, 1, 1, 1, :formBusinessKey, :name, :description, 0)")
                                .setParameter("dataObjectId", dataObjectId)
                                .setParameter("title", formTitle)
                                .setParameter("name", formName)
                                .setParameter("formBusinessKey", formBusinessKey)
                                .setParameter("description", formDescription)
                                .executeForKey("DATA_FORM_ID")).longValue();
                    }else{
                        dataFormId = getNextHibernateSequenceId(etk);

                        etk.createSQL("INSERT INTO etk_data_form(data_form_id, data_object_id, title, instructions, default_form, enable_spell_checker, search_form, layout_type, business_key, name, description, offline_form) VALUES(:dataFormId, :dataObjectId, :title, NULL, 1, 1, 1, 1, :formBusinessKey, :name, :description, 0)")
                        .setParameter("dataFormId", dataFormId)
                        .setParameter("dataObjectId", dataObjectId)
                        .setParameter("title", formTitle)
                        .setParameter("name", formName)
                        .setParameter("formBusinessKey", formBusinessKey)
                        .setParameter("description", formDescription)
                        .execute();
                    }

                    /* ETK_FORM_CONTROL */
                    {
                        /* This variable holds the y coordinate of the field in form builder.
                         * It needs to be incremented as we loop through the fields since it depends on the
                         * fields above it. */
                        long y = 0;

                        for(int index = 0; index < dataElements.size(); index++){

                            final Map<String, Object> dataElement = dataElements.get(index);

                            final long formControlId;

                            if(Utility.isSqlServer(etk)){
                                formControlId = etk.createSQL("INSERT INTO etk_form_control(form_control_type, name, data_form_id, display_order, label, read_only, height, width, x, y, business_key, description, tooltip_text, mutable_read_only) VALUES (:formControlType, :name, :dataFormId, :displayOrder, :label, :readOnly, :height, :width, :x, :y, :businessKey, :description, :tooltipText, :mutableReadOnly)")
                                        .setParameter("dataFormId", dataFormId)
                                        .setParameter("businessKey", String.format("%s.control.%s", formBusinessKey, dataElement.get("elementName")))
                                        .setParameter("label", dataElement.get("name"))
                                        .setParameter("name", dataElement.get("elementName"))
                                        .setParameter("formControlType",
                                                ((FormControlType) dataElement.get("formControlType"))
                                                .getEntellitrakName())
                                        .setParameter("displayOrder", index + 1)
                                        .setParameter("width", dataElement.get("width"))
                                        .setParameter("height", dataElement.get("height"))
                                        .setParameter("x", 0)
                                        .setParameter("y", y)
                                        .setParameter("description", String.format("%s form control", dataElement.get("name")))
                                        .setParameter("readOnly", 0)
                                        .setParameter("tooltipText", null)
                                        .setParameter("mutableReadOnly", 0)
                                        .executeForKey("FORM_CONTROL_ID");
                            }else{
                                formControlId = getNextHibernateSequenceId(etk);

                                etk.createSQL("INSERT INTO etk_form_control(form_control_id, form_control_type, name, data_form_id, display_order, label, read_only, height, width, x, y, business_key, description, tooltip_text, mutable_read_only) VALUES (:formControlId, :formControlType, :name, :dataFormId, :displayOrder, :label, :readOnly, :height, :width, :x, :y, :businessKey, :description, :tooltipText, :mutableReadOnly)")
                                .setParameter("formControlId", formControlId)
                                .setParameter("dataFormId", dataFormId)
                                .setParameter("businessKey", String.format("%s.control.%s", formBusinessKey, dataElement.get("elementName")))
                                .setParameter("label", dataElement.get("name"))
                                .setParameter("name", dataElement.get("elementName"))
                                .setParameter("formControlType",
                                        ((FormControlType) dataElement.get("formControlType")).getEntellitrakName())
                                .setParameter("displayOrder", index + 1)
                                .setParameter("width", dataElement.get("width"))
                                .setParameter("height", dataElement.get("height"))
                                .setParameter("x", 0)
                                .setParameter("y", y)
                                .setParameter("description", String.format("%s form control", dataElement.get("name")))
                                .setParameter("readOnly", 0)
                                .setParameter("tooltipText", null)
                                .setParameter("mutableReadOnly", 0)
                                .execute();
                            }

                            y += ((Number) dataElement.get("height")).longValue() + FORM_VERTICAL_PADDING;

                            dataElement.put("formControlId", formControlId);
                        }
                    }

                    /* ETK_FORM_CTL_ELEMENT_BINDING */
                    for(final Map<String, Object> dataElement : dataElements){
                        etk.createSQL("INSERT INTO etk_form_ctl_element_binding(form_control_id, data_element_id) VALUES(:formControlId, :dataElementId)")
                            .setParameter("formControlId", dataElement.get("formControlId"))
                            .setParameter("dataElementId", dataElement.get("dataElementId"))
                            .execute();
                    }

                    if(generateLookup){
                        lookupDefinitionId = generateDataObjectLookup(etk, dataObjectId, lookupName);
                    }else{
                        lookupDefinitionId = null;
                    }
                }
            }

            final boolean persistFormValues = "generate".equals(requestedAction) && error != null;

            response.put("name", JsonUtilities.encode(persistFormValues ? name : ""));
            response.put("objectName", JsonUtilities.encode(persistFormValues ? objectName : ""));
            response.put("businessKeySegment", JsonUtilities.encode(persistFormValues ? businessKeySegment : ""));
            response.put("description", JsonUtilities.encode(persistFormValues ? description : ""));
            response.put("label", JsonUtilities.encode(persistFormValues ? label : ""));
            response.put("tableName", JsonUtilities.encode(persistFormValues ? tableName : ""));
            response.put("generateLookup", JsonUtilities.encode(persistFormValues ? generateLookup : false));
            response.put("lookupName", JsonUtilities.encode(persistFormValues ? lookupName : ""));

            response.put("dataElements", JsonUtilities.encode(dataElements));
            response.put("dataObjectId", JsonUtilities.encode(dataObjectId));
            response.put("dataFormId", JsonUtilities.encode(dataFormId));
            response.put("dataViewId", JsonUtilities.encode(dataViewId));
            response.put("lookupDefinitionId", JsonUtilities.encode(lookupDefinitionId));
            response.put("error", JsonUtilities.encode(error));

            return response;

        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Gets the next id which entellitrak uses as the primary key for core tables.
     *
     * @param etk entellitrak execution context.
     * @return The next id
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static Long getNextHibernateSequenceId(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException{
        return ((Number) etk.createSQL("SELECT HIBERNATE_SEQUENCE.NEXTVAL FROM DUAL")
                .fetchObject()).longValue();
    }

    /**
     * Utility method which just converts the passed in values into a Map.
     *
     * @param name ETK_DATA_ELEMENT.NAME
     * @param columnName ETK_DATA_ELEMENT.COLUMN_NAME
     * @param dataElementType ETK_DATA_ELEMENT.DATA_TYPE
     * @param elementName ETK_DATA_ELEMENT.ELEMENT_NAME
     * @param dataSize ETK_DATA_ELEMENT.DATA_SIZE
     * @param futureDatesAllowed ETK_DATA_ELEMENT.FUTURE_DATES_ALLOWED
     * @param required ETK_DATA_ELEMENT.REQUIRED
     * @param description ET_DATA_ELEMENT.DESCRIPTION
     * @param width ETK_DATA_ELEMENT.WIDTH
     * @param height ETK_DATA_ELEMENT.HEIGHT
     * @param formControlType ETK_FORM_CONTROL.FORM_CONTROL_TYPE
     * @return Map containing the given fields.
     */
    private static Map<String, Object> generateDefaultDataElement(
            final String name,
            final String columnName,
            final DataElementType dataElementType,
            final String elementName,
            final int dataSize,
            final boolean futureDatesAllowed,
            final DataElementRequiredLevel required,
            final String description,
            final int width,
            final int height,
            final FormControlType formControlType){
        return Utility.arrayToMap(String.class, Object.class, new Object[][]{
            {"name", name},
            {"columnName", columnName},
            {"dataElementType", dataElementType},
            {"elementName", elementName},
            {"dataSize", dataSize},
            {"futureDatesAllowed", futureDatesAllowed},
            {"required", required},
            {"description", description},
            {"width", width},
            {"height", height},
            {"formControlType", formControlType}
        });
    }

    /**
     * Returns default data that we know about Name, Code, Order, Start Date and End Date.
     * More information needs to be added to the Data Elements before they can actually be inserted into the database.
     *
     * @return List of Default Data Elements for Name, Code, Order, Start Date and End Date
     */
    private static List<Map<String, Object>> getDefaultDataElements(){
        final List<Map<String, Object>> dataElements = new LinkedList<>();

        dataElements.add(generateDefaultDataElement("Name", "C_NAME", DataElementType.TEXT, "name", DEFAULT_TEXT_SIZE,
                false, DataElementRequiredLevel.REQUIRED, "Name", DEFAULT_FORM_ELEMENT_WIDTH,
                DEFAULT_FORM_ELEMENT_HEIGHT, FormControlType.TEXT));
        dataElements.add(generateDefaultDataElement("Code", "C_CODE", DataElementType.TEXT, "code", DEFAULT_TEXT_SIZE,
                false, DataElementRequiredLevel.REQUIRED, "Code", DEFAULT_FORM_ELEMENT_WIDTH,
                DEFAULT_FORM_ELEMENT_HEIGHT, FormControlType.TEXT));
        dataElements.add(generateDefaultDataElement("Order", "C_ORDER", DataElementType.NUMBER, "order", 0, false,
                DataElementRequiredLevel.NOT_REQUIRED, "Order", DEFAULT_FORM_ELEMENT_WIDTH, DEFAULT_FORM_ELEMENT_HEIGHT,
                FormControlType.TEXT));
        dataElements.add(generateDefaultDataElement("Start Date", "ETK_START_DATE", DataElementType.DATE, "startDate",
                0, true, DataElementRequiredLevel.NOT_REQUIRED,
                "Start Date is used to define when a reference record becomes active.  A null/empty value means that the record will always be active unless an End Date is set.",
                DEFAULT_FORM_DATE_WIDTH, DEFAULT_FORM_ELEMENT_HEIGHT, FormControlType.DATE));
        dataElements.add(generateDefaultDataElement("End Date", "ETK_END_DATE", DataElementType.DATE, "endDate", 0,
                true, DataElementRequiredLevel.NOT_REQUIRED,
                "End Date is used to define when a reference record becomes inactive.  A null/empty value means that the record will always be active unless a Start Date is set.",
                DEFAULT_FORM_DATE_WIDTH, DEFAULT_FORM_ELEMENT_HEIGHT, FormControlType.DATE));

        return dataElements;
    }

    /**
     * Generates a Lookup of type Data Object for the given Data Object. It uses name for Display, Order for Order,
     * and Start Date and End Date for Start and End Date.
     *
     * @param etk entellitrak Execution Context
     * @param dataObjectId id of the Data Object for which the lookup will be created
     * @param lookupName Optional Custom Lookup Name
     * @return The id of the newly created lookup
     *
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static long generateDataObjectLookup(final PageExecutionContext etk, final long dataObjectId, final String lookupName)
            throws IncorrectResultSizeDataAccessException{

        final long lookupId;

        final Map<String, Object> dataObjectInfo = etk.createSQL("SELECT (SELECT dataElement.data_element_id FROM etk_data_element dataElement WHERE dataElement.data_object_id = dataObject.data_object_id AND dataElement.element_name = 'name') NAME_ID, (SELECT dataElement.data_element_id FROM etk_data_element dataElement WHERE dataElement.data_object_id = dataObject.data_object_id AND dataElement.element_name = 'order') ORDER_ID, (SELECT dataElement.data_element_id FROM etk_data_element dataElement WHERE dataElement.data_object_id = dataObject.data_object_id AND dataElement.element_name = 'startDate') START_DATE_ID, (SELECT dataElement.data_element_id FROM etk_data_element dataElement WHERE dataElement.data_object_id = dataObject.data_object_id AND dataElement.element_name = 'endDate') END_DATE_ID, dataObject.name OBJECT_NAME, dataobject.business_key FROM etk_data_object dataObject WHERE dataObject.data_object_id = :dataObjectId")
                .setParameter("dataObjectId", dataObjectId)
                .fetchMap(); /* NAME_ID, ORDER_ID, START_DATE_ID, END_DATE_ID, OBJECT_NAME, BUSINESS_KEY */

        final String dataObjectName = (String) dataObjectInfo.get("OBJECT_NAME");
        final String dataObjectBusinessKey = (String) dataObjectInfo.get("BUSINESS_KEY");
        final String businessKeyPart = dataObjectBusinessKey.substring(dataObjectBusinessKey.indexOf('.') + 1);
        // TODO: lookupBusinessKey should now be based off of the lookup name, not the object name?
        // TODO: Should the UI include a separate read-only field for lookup business key?
        // TODO: Does any validation have to be done against lookup name?
        final String lookupBusinessKey = String.format("lookup.%s", businessKeyPart);

        final Map<String, Object> queryParameters = Utility.arrayToMap(String.class, Object.class, new Object[][]{
            {"lookupSourceType", LookupSourceType.DATA_OBJECT_LOOKUP.getEntellitrakNumber()},
            {"dataObjectId", dataObjectId},
            {"valueElementId", null},
            {"displayElementId", dataObjectInfo.get("NAME_ID")},
            {"orderByElementId", dataObjectInfo.get("ORDER_ID")},
            {"ascendingOrder", null},
            {"startDateElementId", dataObjectInfo.get("START_DATE_ID")},
            {"endDateElementId", dataObjectInfo.get("END_DATE_ID")},
            {"sqlScriptObjectId", null},
            {"pluginRegistrationId", null},
            {"valueReturnType", null},
            {"trackingConfigId", Utility.getTrackingConfigIdNext(etk)},
            {"businessKey", lookupBusinessKey},
            {"name", StringUtility.isNotBlank(lookupName) ? lookupName : dataObjectName},
            {"description", String.format("Lookup of %s", dataObjectName)},
            {"enableCaching", 0}
        });

        if(Utility.isSqlServer(etk)){
            lookupId = etk.createSQL("INSERT INTO etk_lookup_definition(lookup_source_type, data_object_id, value_element_id, display_element_id, order_by_element_id, ascending_order, start_date_element_id, end_date_element_id, sql_script_object_id, plugin_registration_id, value_return_type, tracking_config_id, business_key, name, description, enable_caching) VALUES(:lookupSourceType, :dataObjectId, :valueElementId, :displayElementId, :orderByElementId, :ascendingOrder, :startDateElementId, :endDateElementId, :sqlScriptObjectId, :pluginRegistrationId, :valueReturnType, :trackingConfigId, :businessKey, :name, :description, :enableCaching)")
                .setParameter(queryParameters)
                .executeForKey("LOOKUP_DEFINITION_ID");
        }else{
            lookupId = getNextHibernateSequenceId(etk);

            queryParameters.put("lookupDefinitionId", lookupId);

            etk.createSQL("INSERT INTO etk_lookup_definition(lookup_definition_id, lookup_source_type, data_object_id, value_element_id, display_element_id, order_by_element_id, ascending_order, start_date_element_id, end_date_element_id, sql_script_object_id, plugin_registration_id, value_return_type, tracking_config_id, business_key, name, description, enable_caching) VALUES(:lookupDefinitionId, :lookupSourceType, :dataObjectId, :valueElementId, :displayElementId, :orderByElementId, :ascendingOrder, :startDateElementId, :endDateElementId, :sqlScriptObjectId, :pluginRegistrationId, :valueReturnType, :trackingConfigId, :businessKey, :name, :description, :enableCaching)")
                .setParameter(queryParameters)
                .execute();
        }

        return lookupId;
    }
}
