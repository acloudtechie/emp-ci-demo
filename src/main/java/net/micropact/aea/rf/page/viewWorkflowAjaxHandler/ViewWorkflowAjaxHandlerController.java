package net.micropact.aea.rf.page.viewWorkflowAjaxHandler;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.legacy.util.StringUtility;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

/**
 * This page is resonsible for saving the state of the workflow graph including the coordinates of all of the states.
 * It takes an XML document through the xmlData parameter that looks like:
 *  <pre>
 *      &lt;rf-states&gt;
 *          &lt;rf-state y-coordinate="178px" x-coordinate="419px" rf-state-id="587"&gt;&lt;/rf-state&gt;
 *          &lt;rf-state y-coordinate="179px" x-coordinate="178px" rf-state-id="588"&gt;&lt;/rf-state&gt;
 *          ...
 *          &lt;rf-start-state workflow-id="586" y-coordinate="84px" x-coordinate="199px"&gt;
 *          &lt;/rf-start-state&gt;
 *      &lt;/rf-states&gt;
 *  </pre>
 * This DTD isn't to good and should probably be changed so that the workflowId is stored above or in the rf-states tag.
 *
 * @author zmiller
 * @see net.micropact.aea.rf.page.viewWorkflow.ViewWorkflowController
 */
public class ViewWorkflowAjaxHandlerController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();

            final String xmlData = etk.getParameters().getSingle("xmlData");

            /*Update the rfStates*/
            getRfStates(xmlData).forEach(rfState ->
            etk.createSQL("UPDATE t_rf_state SET c_x_coordinate = :xCoordinate, c_y_coordinate = :yCoordinate WHERE id = :rfStateId")
            .setParameter("xCoordinate", rfState.get("x-coordinate"))
            .setParameter("yCoordinate", rfState.get("y-coordinate"))
            .setParameter("rfStateId", rfState.get("rf-state-id"))
            .execute());

            /*Update the startState*/
            final Map<String, String> startState = getStartState(xmlData);
            etk.createSQL("UPDATE t_rf_workflow SET c_start_state_x_coordinate = :xCoordinate, c_start_state_y_coordinate = :yCoordinate WHERE id = :workflowId")
            .setParameter("workflowId", startState.get("workflow-id"))
            .setParameter("xCoordinate", startState.get("x-coordinate"))
            .setParameter("yCoordinate", startState.get("y-coordinate"))
            .execute();

            /* Only for debugging. can change to whatever you want */
            response.put("out", startState);

            return response;
        } catch (final Exception e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Parses the XML document for the information related to the RF State objects and converts them to a list of maps
     * so that they are easier to work with.
     *
     * @param xmlString XML document containing information on the RF States.
     * @return A List of Maps where each Map represents an RF State.
     *     The keys in the map are the same as the XML attributes.
     * @throws SAXException If there was an underlying {@link SAXException}.
     * @throws IOException If there was an underlying {@link IOException}.
     * @throws ParserConfigurationException If there was an underlying {@link ParserConfigurationException}.
     */
    private static List<Map<String, String>> getRfStates(final String xmlString)
            throws SAXException, IOException, ParserConfigurationException{

        final List<Map<String, String>> returnList = new LinkedList<>();

        if (StringUtility.isNotBlank(xmlString)){
            final Document xmlDocument = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xmlString)));
            final NodeList parameters = xmlDocument.getElementsByTagName("rf-state");
            for(int i = 0; i < parameters.getLength(); i++){
                final Node parameterNode = parameters.item(i);
                final NamedNodeMap attributes = parameterNode.getAttributes();

                final Map<String, String> attrs = new HashMap<>();
                attrs.put("x-coordinate", attributes.getNamedItem("x-coordinate").getFirstChild().getNodeValue());
                attrs.put("y-coordinate", attributes.getNamedItem("y-coordinate").getFirstChild().getNodeValue());
                attrs.put("rf-state-id", attributes.getNamedItem("rf-state-id").getFirstChild().getNodeValue());

                returnList.add(attrs);
            }
        }
        return returnList;
    }

    /**
     * Retrieves information regarding the 'Start State' from the XML document.
     *
     * @param xmlString XML document containing all the information regarding states.
     * @return A map representing the starting state. The keys are the same as the xml attribute names.
     * @throws SAXException If there was an underlying {@link SAXException}.
     * @throws IOException If there was an underlying {@link IOException}.
     * @throws ParserConfigurationException If there was an underlying {@link ParserConfigurationException}.
     */
    private static Map<String, String> getStartState(final String xmlString)
            throws SAXException, IOException, ParserConfigurationException{
        final Map<String, String> returnMap = new HashMap<>();
        if(StringUtility.isNotBlank(xmlString)){
            final Document xmlDocument = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xmlString)));
            final Node startState = xmlDocument.getElementsByTagName("rf-start-state").item(0);
            final NamedNodeMap attributes = startState.getAttributes();
            returnMap.put("x-coordinate", attributes.getNamedItem("x-coordinate").getFirstChild().getNodeValue());
            returnMap.put("y-coordinate", attributes.getNamedItem("y-coordinate").getFirstChild().getNodeValue());
            returnMap.put("workflow-id", attributes.getNamedItem("workflow-id").getFirstChild().getNodeValue());

        }
        return returnMap;
    }
}
