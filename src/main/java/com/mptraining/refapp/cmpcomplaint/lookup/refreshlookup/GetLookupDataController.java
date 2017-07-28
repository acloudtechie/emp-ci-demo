package com.mptraining.refapp.cmpcomplaint.lookup.refreshlookup;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.lookup.For;
import com.entellitrak.lookup.Lookup;
import com.entellitrak.lookup.LookupResult;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

/**
 * This class is the controller code for a page which will take information about a form control and the serialized form
 * and return the lookup data which would correspond to that field by using the core LookupServices.
 *
 * This can be used to avoid using refreshTrackingForm by just getting the values for lookups
 * which need to be refreshed.
 *
 * @author zmiller
 */
public class GetLookupDataController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {

            final TextResponse response = etk.createTextResponse();

            final String formControlName = etk.getParameters().getSingle("FORM_CONTROL_NAME");
            final String executeForString = etk.getParameters().getSingle("EXECUTE_FOR");
            final String dataFormKey = etk.getParameters().getSingle("dataFormKey");

            final List<Map<String, Object>> formControls = etk.createSQL("SELECT dataObject.OBJECT_NAME, formControl.name FORM_CONTROL_NAME, formControl.FORM_CONTROL_TYPE, formControl.FORM_CONTROL_ID, CASE WHEN EXISTS (SELECT * FROM etk_form_ctl_element_binding elementBinding WHERE elementBinding.form_control_id = formControl.form_control_id ) THEN 1 ELSE 0 END IS_ELEMENT FROM etk_data_form dataForm JOIN etk_data_object dataObject ON dataObject.data_object_id = dataForm.data_object_id JOIN etk_form_control formControl ON formControl.data_form_id = dataForm.data_form_id WHERE dataObject.tracking_config_id = (SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive ) AND dataForm.business_key = :dataFormKey")
                    .setParameter("dataFormKey", dataFormKey)
                    .fetchList();

            addPrefixFormControlName(formControls);

            final String lookupBusinessKey = getLookupBusinessKey(etk, formControlName, formControls);
            final Lookup lookup = etk.getLookupService().getLookup(lookupBusinessKey);

            setGlobalParameters(etk, lookup);
            setFormControlParameters(etk, lookup, formControls);

            response.setContentType("text/xml");

            response.put("out", buildXml(lookup.execute(determineFor(etk, For.valueOf(executeForString)))));
            return response;

        } catch (final TransformerException | ParserConfigurationException | IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * This takes the {@link For} that the lookup would like to be run with and analyzes whether the it is enabled
     * in this particular site. If it is not enabled for the site, then it will return the appropriate one to
     * use instead
     *
     * @param etk entellitrak execution context
     * @param requestedFor The {@link For} that the user wants to run
     * @return The {@link For} that should be used for this context
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static For determineFor(final ExecutionContext etk, final For requestedFor)
            throws IncorrectResultSizeDataAccessException {
        switch (requestedFor) {
            case TRACKING:
                return For.TRACKING;
            case SINGLE:
                return "true".equals(etk.createSQL("SELECT value FROM etk_system_preference WHERE name = :preferenceName")
                        .setParameter("preferenceName", "enableSingleResultLookupContext")
                        .returnEmptyResultSetAs("false")
                        .fetchString())
                        ? For.SINGLE
                        : For.TRACKING;
            default:
                throw new IllegalArgumentException(String.format("Invalid Parameter For: %s", requestedFor));
        }
    }

    /**
     * This sets the lookup parameters for other elements that were on the form. This includes both data elements
     * and unbound form controls.
     *
     * @param etk entellitrak execution context
     * @param lookup lookup to set the parameters on
     * @param formControls A list of form control descriptions for form controls which were on the form.
     */
    private static void setFormControlParameters(final PageExecutionContext etk,
            final Lookup lookup,
            final List<Map<String, Object>> formControls){
        for(final Map<String, Object> formControl : formControls){

            final String prefixedFormControlName = (String) formControl.get("prefixedFormControlName");
            final String currentFormControlName = (String) formControl.get("FORM_CONTROL_NAME");
            Object parameterValue;

            if("checkbox".equals(formControl.get("FORM_CONTROL_TYPE"))){
                final List<String> paramVals = etk.getParameters().getField(prefixedFormControlName);
                parameterValue = paramVals == null ? Collections.emptyList() : paramVals;
            }else{
                parameterValue = etk.getParameters().getSingle(prefixedFormControlName);
            }
            lookup.set(currentFormControlName, parameterValue);
        }
    }

    /**
     * Gets the business key of the lookup which is associated with a particular form control.
     *
     * @param etk entellitrak execution context
     * @param formControlName The form control which we should find the lookup for
     * @param formControls A list of form controls which are on this particular form.
     *          The Maps should have a key "prefixedFormControlName" which contains the name entellitrak gives to the
     *          control when it is on the form. This is because data elements and unbound fields are named differently
     *          by entellitrak.
     * @return The business key of the lookup
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static String getLookupBusinessKey(final ExecutionContext etk,
            final String formControlName,
            final List<Map<String, Object>> formControls)
                    throws IncorrectResultSizeDataAccessException {
        for(final Map<String, Object> formControl : formControls){
            if(formControlName.equals(formControl.get("prefixedFormControlName"))){
                final Object formControlId = formControl.get("FORM_CONTROL_ID");

                return etk.createSQL(isFormControlElement(formControl) ? "SELECT lookupDefinition.business_key FROM etk_form_ctl_element_binding elementBinding JOIN etk_data_element dataElement ON dataElement.data_element_id = elementBinding.data_element_id JOIN etk_lookup_definition lookupDefinition ON lookupDefinition.lookup_definition_id = dataElement.lookup_definition_id WHERE elementBinding.form_control_id = :formControlId"
                          : "SELECT lookupDefinition.business_key FROM etk_form_ctl_lookup_binding lookupBinding JOIN etk_lookup_definition lookupDefinition ON lookupDefinition.lookup_definition_id = lookupBinding.lookup_definition_id WHERE lookupBinding.form_control_id = :formControlId")
                          .setParameter("formControlId", formControlId)
                          .fetchString();
            }
        }
        throw new RuntimeException(String.format("Could not find formControl with name %s", formControlName));
    }

    /**
     * Reads whether or not a form control is a data element.
     *
     * @param formControl The form control
     * @return Whether the form control is a data element
     */
    private static boolean isFormControlElement(final Map<String, Object> formControl){
        return 1 == ((Number) formControl.get("IS_ELEMENT")).intValue();
    }

    /**
     * This function modifies formControls to add a key "prefixedFormControlName" which has the actual name of the
     * form control. This is necessary because entellitrak treats data elements and unbound fields differently.
     * For data elements it concatenates the data object and data element names to produce a new name, but for unbound
     * fields it does not.
     *
     * @param formControls List of form controls which are on the form
     */
    private static void addPrefixFormControlName(final List<Map<String, Object>> formControls) {
        for(final Map<String, Object> formControl : formControls){
            final String currentFormControlName = (String) formControl.get("FORM_CONTROL_NAME");

            final String prefixedFormControlName = isFormControlElement(formControl)
                    ? String.format("%s_%s", formControl.get("OBJECT_NAME"), formControl.get("FORM_CONTROL_NAME"))
                    : currentFormControlName;

            formControl.put("prefixedFormControlName", prefixedFormControlName);
        }
    }

    /**
     * Sets the parameters which are available on all forTracking lookups such as {?parentId} or {?currentUser.id}.
     *
     * @param etk entellitrak execution context
     * @param lookup The lookup to set the parameters on
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static void setGlobalParameters(final PageExecutionContext etk,
            final Lookup lookup) throws IncorrectResultSizeDataAccessException {

        final Map<String, Object> dataObjectInfo = etk.createSQL("SELECT TABLE_NAME, OBJECT_NAME FROM etk_data_object dataObject WHERE dataObject.business_key = :dataObjectKey AND tracking_config_id = (SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive)")
                .setParameter("dataObjectKey", etk.getParameters().getSingle("dataObjectKey"))
                .fetchMap();

        lookup.set("currentUser.id", etk.getCurrentUser().getId());
        lookup.set("currentUser.roleId", etk.getCurrentUser().getRole().getId());
        lookup.set("currentUser.ouId", etk.getCurrentUser().getHierarchy().getId());
        lookup.set("assignmentRoleId", null);
        lookup.set("trackingId", etk.getParameters().getSingle("trackingId"));
        lookup.set("baseId", etk.getParameters().getSingle("baseId"));
        lookup.set("parentId", etk.getParameters().getSingle("parentId"));
        lookup.set("tableName", dataObjectInfo.get("TABLE_NAME"));
        lookup.set("dataObjectName", dataObjectInfo.get("OBJECT_NAME"));
        lookup.set("businessKey", etk.getParameters().getSingle("dataObjectKey"));
    }

    /**
     * Transforms a List of lookup results to an XML representation.
     * The XML has the structure:
     *
     * &lt;lookup&gt;
     *   &lt;lookupResult value=&quot;val&quot; display=&quot;disp&quot;/&gt;
     *   ...
     * &lt;/lookup&gt;
     *
     * @param lookupResults List of results which should be transformed to XML
     * @return An XML representation of the lookup data
     * @throws TransformerException If there was an underlying {@link TransformerException}
     * @throws ParserConfigurationException If there was an underlying {@link ParserConfigurationException}
     */
    private static String buildXml(final List<LookupResult> lookupResults)
            throws TransformerException, ParserConfigurationException{
        final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        final Document doc = docBuilder.newDocument();
        final Element lookupsElement = doc.createElement("lookup");
        doc.appendChild(lookupsElement);

        for(final LookupResult lookup : lookupResults){
            final Element lookupElement = doc.createElement("lookupResult");

            lookupElement.setAttribute("value", lookup.getValue());
            lookupElement.setAttribute("display", lookup.getDisplay());

            lookupsElement.appendChild(lookupElement);
        }

        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        final Transformer transformer = transformerFactory.newTransformer();
        final DOMSource source = new DOMSource(doc);
        final ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
        final StreamResult result = new StreamResult(resultStream);

        transformer.transform(source, result);
        return new String(resultStream.toByteArray());
    }
}
