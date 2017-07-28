package net.micropact.aea.rf.utility.dynamicParameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.entellitrak.ApplicationException;
import com.entellitrak.DataEventType;
import com.entellitrak.DataObjectEventContext;
import com.entellitrak.InputValidationException;
import com.entellitrak.configuration.DataElement;
import com.entellitrak.dynamic.DataObjectInstance;
import com.entellitrak.dynamic.RfParameterType;
import com.entellitrak.form.FormEventContext;
import com.entellitrak.legacy.util.StringUtility;

import net.micropact.aea.core.utility.DynamicObjectConfigurationUtils;
import net.micropact.aea.core.utility.StringEscapeUtils;
import net.micropact.aea.utility.ImportExportUtility;
import net.micropact.aea.utility.Utility;

/**
 * Contains the utility methods for managing the dynamic script parameters which are used on RF Workflow Effect
 * and RF Transition. In order to use it, you must use
 * {@link DynamicParametersUtility#initializeParametersElement(FormEventContext, IDynamicParameterUseInfo)}
 * in the form Read Event and
 * {@link DynamicParametersUtility#saveParameters(DataObjectEventContext, long, IDynamicParameterUseInfo)}
 * in the Data Object Event handler.
 *
 * @author zmiller
 */
public final class DynamicParametersUtility {

    /**
     * Utility classes do not need constructors.
     */
    private DynamicParametersUtility(){}

    /**
     * This method will populate an unbound "parameters" text area on the data form with the parameter values which
     * are currently stored in the database. It should be called in the Read Event of a Data Form.
     *
     * @param etk entellitrak execution context
     * @param parameterInfo definition of this implementation of parameters
     * @throws ApplicationException If there was an underlying exception
     */
    public static void initializeParametersElement(final FormEventContext etk,
            final IDynamicParameterUseInfo parameterInfo)
                    throws ApplicationException{
        try{
            /* So apparently refreshTrackingForm fires the form event listeners again so we have to check if parameters
             * already has a value. */
            if(null == etk.getElement("parameters").getValue()){
                final long trackingId = etk.getTrackingId();

                final String parameterIdColumn = parameterInfo.getParameterReferenceElement().getColumnName();

                final List<Map<String, Object>> parameters = etk.createSQL(
                        String.format("SELECT parameterValue.%s, parameterValue.C_VALUE FROM %s parameterValue WHERE parameterValue.id_parent = :trackingId ORDER BY parameterValue.id",
                                parameterIdColumn,
                                parameterInfo.getParameterReferenceElement().getDataObject().getTableName()))
                        .setParameter("trackingId", trackingId)
                        .fetchList();

                final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                final Document doc = docBuilder.newDocument();
                final Element parametersElement = doc.createElement("parameters");
                doc.appendChild(parametersElement);

                for(final Map<String, Object> parameter : parameters){
                    final Element parameterElement = doc.createElement("parameter");

                    final Element idElement = doc.createElement("parameterid");
                    idElement.appendChild(doc.createTextNode(parameter.get(parameterIdColumn).toString()));
                    parameterElement.appendChild(idElement);

                    final Element valueElement = doc.createElement("value");
                    valueElement.appendChild(doc.createTextNode((String) parameter.get("C_VALUE")));
                    parameterElement.appendChild(valueElement);

                    parametersElement.appendChild(parameterElement);
                }

                final TransformerFactory transformerFactory = TransformerFactory.newInstance();
                final Transformer transformer = transformerFactory.newTransformer();
                final DOMSource source = new DOMSource(doc);
                final ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
                final StreamResult result = new StreamResult(resultStream);

                transformer.transform(source, result);

                etk.getElement("parameters").setValue(new String(resultStream.toByteArray()));
            }
        } catch (final ParserConfigurationException | TransformerException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * This will take an XmlString as specified at the top of this file and convert it to a List of Maps
     * where the Keys are the attribute names and the Values are the attribute values.
     *
     * @param xmlString the XML to be parsed
     * @return A representation of xmlString as a List of Maps.
     * @throws SAXException If there was an underlying {@link SAXException}.
     * @throws IOException If there was an underlying {@link IOException}.
     * @throws ParserConfigurationException If there was an underlying {@link ParserConfigurationException}.
     */
    private static List<Map<String, String>> parseParameterXml(final String xmlString)
            throws SAXException, IOException, ParserConfigurationException{

        final List<Map<String, String>> returnList = new LinkedList<>();

        if (StringUtility.isNotBlank(xmlString)){
            final Document xmlDocument = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xmlString)));
            /* Get the tags */
            final NodeList parameters = xmlDocument.getElementsByTagName("parameter");
            for(int i = 0; i < parameters.getLength(); i++){
                final Node parameterNode = parameters.item(i);
                final Node idNode = parameterNode.getFirstChild();
                final Node valueNode = parameterNode.getLastChild();

                if(valueNode.getFirstChild() != null && !"".equals(valueNode.getFirstChild().getNodeValue())){
                    /* Add those attributes to the Map */
                    final Map<String, String> attrs = new HashMap<>();
                    attrs.put("parameterid", idNode.getFirstChild().getNodeValue());
                    attrs.put("value", valueNode.getFirstChild().getNodeValue());

                    returnList.add(attrs);
                }
            }
        }
        return returnList;
    }

    /**
     * This method saves the parameters coming from the unbound text area into the database. It should be called from
     * the Data Object Event Handler on the object.
     *
     * @param etk entellitrak execution context
     * @param parameterDefiningParentId The id of the parent object containing the definition of the parameters.
     *          In the case of RfWorkflowEffect, the id would be the RF Script's trackingId which is referenced.
     * @param dynamicParameterUseInfo
     *          Description of the objects involved in the use of dynamic parameters
     * @throws SAXException If there was an underlying {@link SAXException}
     * @throws IOException If there was an underlying {@link IOException}
     * @throws ParserConfigurationException If there was an underlying {@link ParserConfigurationException}
     */
    public static void saveParameters(final DataObjectEventContext etk,
            final long parameterDefiningParentId,
            final IDynamicParameterUseInfo dynamicParameterUseInfo)
                    throws SAXException, IOException, ParserConfigurationException{
        final long currentTrackingId = etk.getNewObject().properties().getId();
        final long baseId = etk.getNewObject().properties().getBaseId();

        final DataElement parameterValueElement = dynamicParameterUseInfo.getParameterReferenceElement();
        final String parameterDefiningTable = dynamicParameterUseInfo.getParameterDefiningObject().getTableName();
        final String parameterValueHoldingTable = parameterValueElement.getDataObject().getTableName();
        final String parameterIdColumn = parameterValueElement.getColumnName();

        if(DataEventType.CREATE == etk.getDataEventType()
                || DataEventType.UPDATE == etk.getDataEventType()){
            /* These are the parameters that the RfScript specifies that it ought to take */
            final List<Map<String, Object>> possibleParameters = etk.createSQL(
                    String.format("SELECT parameter.id PARAMETERID, parameter.c_required REQUIRED, parameter.c_name PARAMETERNAME FROM %s parameter WHERE parameter.id_parent = :parameterDefiningParentId",
                            parameterDefiningTable))
                    .setParameter("parameterDefiningParentId", parameterDefiningParentId)
                    .fetchList() /* PARAMETERID, REQUIRED, PARAMETERNAME */;

            /* submittedParameters contain the parameters that are coming from the form */
            final List<Map<String, String>> submittedParameters =
                    DynamicParametersUtility.parseParameterXml(etk.getForm().getValue("parameters"));

            /* check to see that if there is a parameter which is required, and isn't in the submitted ones,
             * we cancel the transaction. */
            for(final Map<String, Object> possibleParameter : possibleParameters){
                if("1".equals(possibleParameter.get("REQUIRED")+"")){
                    final String value =
                            ImportExportUtility.lookupValueInListOfMaps(
                                    submittedParameters,
                                    "parameterid",
                                    possibleParameter.get("PARAMETERID").toString(),
                                    "value");

                    if(StringUtility.isBlank(value)){
                        Utility.cancelTransactionMessage(etk,
                                StringEscapeUtils.escapeHtml(String.format("Parameter \"%s\" is required",
                                        possibleParameter.get("PARAMETERNAME"))));
                    }
                }
            }

            /* A check for multiple parameters could also be done here */

            /* Delete our child ScriptParameterValues and replace them with our new children which are
             * determined from the parameters field which holds an XML description */
            etk.createSQL(String.format("DELETE FROM %s WHERE id_parent = :currentTrackingId",
                    parameterValueHoldingTable))
            .setParameter("currentTrackingId", currentTrackingId)
            .execute();

            for(final Map<String, String> newParameter : submittedParameters){

                /* Insert into RfScriptParameter */
                etk.createSQL(String.format(Utility.isSqlServer(etk) ? "INSERT INTO %s(id_base, id_parent, %s, c_value) VALUES(:idBase, :idParent, :parameterId, :value)"
                                                                       : "INSERT INTO %s(id, id_base, id_parent, %s, c_value) VALUES(OBJECT_ID.NEXTVAL, :idBase, :idParent, :parameterId, :value)",
                                                                       parameterValueHoldingTable,
                                                                       parameterIdColumn))
                .setParameter("idBase", baseId)
                .setParameter("idParent", currentTrackingId)
                .setParameter("parameterId",
                        newParameter.get("parameterid"))
                .setParameter("value", newParameter.get("value"))
                .execute();
            }

            /* Note: The way validation is being done should really be refactored. Among other things, if there are
             * multiple types of errors (required & natural number) the errors do not get displayed to the user
             * in the same order as the fields. */

            /* Do natural number validation. */
            for(final Map<String, Object> naturalNumber : etk.createSQL(String.format("SELECT parameterDefiningTable.c_name NAME, parameterValueHoldingTable.c_value VALUE FROM %s parameterValueHoldingTable JOIN %s parameterDefiningTable ON parameterDefiningTable.id = parameterValueHoldingTable.%s JOIN t_rf_parameter_type parameterType ON parameterType.id = parameterDefiningTable.c_type WHERE parameterValueHoldingTable.id_parent = :idParent AND parameterType.c_code = 'naturalNumber' ORDER BY parameterValueHoldingTable.id",
                    parameterValueHoldingTable,
                    parameterDefiningTable,
                    parameterIdColumn))
                    .setParameter("idParent", currentTrackingId)
                    .fetchList()){
                final String stringValue = (String) naturalNumber.get("VALUE");
                if(!isNaturalNumber(stringValue)){
                    Utility.cancelTransactionMessage(etk, String.format("%s must be a natural number",
                            StringEscapeUtils.escapeHtml((String) naturalNumber.get("NAME"))));
                }
            }
        }
    }

    /**
     * Predicate for testing whether a string represents a natural number (Integer greater than zero).
     *
     * @param stringValue string to check if it is a natural number
     * @return whether the number is a natural number
     */
    private static boolean isNaturalNumber(final String stringValue) {
        try{
            final long longValue = Long.parseLong(stringValue, 10);
            return longValue >= 0;
        }catch(final NumberFormatException e){
            return false;
        }
    }

    /**
     * Ensures that the Lookup field is required if the type is lookup.
     * To be used in the data object event handler of the objects where parameters themselves are defined.
     *
     * @param etk entellitrak execution context
     * @throws InputValidationException If there was an underlying {@link InputValidationException}
     * @throws ClassNotFoundException If there was an underlying {@link ClassNotFoundException}
     */
    public static void validateLookupField(final DataObjectEventContext etk) throws InputValidationException, ClassNotFoundException {
        final DataEventType dataEventType = etk.getDataEventType();
        final DataObjectInstance newObject = etk.getNewObject();

        final RfParameterType parameterType = etk.getDynamicObjectService()
                .get(RfParameterType.class, newObject.get(Number.class, "type").longValue());

        if(dataEventType == DataEventType.CREATE
                || dataEventType == DataEventType.UPDATE){
            if("lookup".equals(parameterType.getCode())){
                if(newObject.get(Number.class, "lookup") == null){
                    Utility.cancelTransactionMessage(etk, "Lookup is required");
                }
            }else{
                final DataObjectInstance instanceForClearingLookup =
                        etk.getDynamicObjectService().get(
                                DynamicObjectConfigurationUtils.getDynamicClass(etk,
                                        newObject.configuration().getBusinessKey()),
                                newObject.properties().getId());
                instanceForClearingLookup.set("lookup", null);
                etk.getDynamicObjectService().createSaveOperation(instanceForClearingLookup)
                .setExecuteEvents(false)
                .save();
            }
        }
    }
}
