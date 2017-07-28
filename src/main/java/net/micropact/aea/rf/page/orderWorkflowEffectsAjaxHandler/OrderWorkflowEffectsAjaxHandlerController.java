package net.micropact.aea.rf.page.orderWorkflowEffectsAjaxHandler;

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
 * This page handles the AJAX saving of a new RF Workflow Effect ordering.
 * It takes an XML document through the xmlData parameter that looks like
 *  <pre>
 *  &lt;rf-workflow&gt;
 *      &lt;workflow-effect id="123" order="40" /&gt;
 *      &lt;workflow-effect id="22" order="20" /&gt;
 *      &lt;workflow-effect id="66" order="30" /&gt;
 *  &lt;/rf-workflow&gt;
 *  </pre>
 *
 * @author zmiller
 * @see net.micropact.aea.rf.page.orderWorkflowEffects.OrderWorkflowEffectsController
 */
public class OrderWorkflowEffectsAjaxHandlerController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();

            final String xmlData = etk.getParameters().getSingle("xmlData");

            /*Update the rfStates*/
            getRfWorkflowEffects(xmlData).stream().forEachOrdered(workflowEffect ->
                etk.createSQL("UPDATE t_rf_workflow_effect SET c_execution_order = :executionOrder WHERE id = :trackingId")
                .setParameter("executionOrder", workflowEffect.get("order"))
                .setParameter("trackingId", workflowEffect.get("id"))
                .execute());

            return response;
        } catch (final Exception e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * This will take the XML as a String and return a List of Maps
     * where each map is one of the workflow-effect entries.
     *
     * @param xmlString The XML to be parsed.
     * @return A List of Maps where the keys of the map are &quot;id&quot; and &quot;order&quot;
     * @throws SAXException If there was an underlying {@link SAXException}
     * @throws IOException If there was an underlying {@link IOException}
     * @throws ParserConfigurationException If there was an underlying {@link ParserConfigurationException}
     */
    private static List<Map<String, String>> getRfWorkflowEffects(final String xmlString)
            throws SAXException, IOException, ParserConfigurationException{

        final List<Map<String, String>> returnList = new LinkedList<>();

        if (StringUtility.isNotBlank(xmlString)){
            final Document xmlDocument = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xmlString)));
            final NodeList parameters = xmlDocument.getElementsByTagName("workflow-effect");
            for(int i = 0; i < parameters.getLength(); i++){
                final Node parameterNode = parameters.item(i);
                final NamedNodeMap attributes = parameterNode.getAttributes();

                final Map<String, String> attrs = new HashMap<>();
                attrs.put("id", attributes.getNamedItem("id").getFirstChild().getNodeValue());
                attrs.put("order", attributes.getNamedItem("order").getFirstChild().getNodeValue());

                returnList.add(attrs);
            }
        }
        return returnList;
    }
}
