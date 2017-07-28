package net.micropact.aea.du.utility.systemPreference;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import net.micropact.aea.utility.ImportExportUtility;

/**
 * This class groups together the logic for serializing and deserializing system preference values so that they may
 * be moved from one system to another.
 *
 * @author zachary.miller
 */
public final class SystemPreferenceMarshaller{

    /**
     * Utility clases do not need constructors.
     */
    private SystemPreferenceMarshaller(){}

    /**
     * Marshalls a collection of system preference values to an XML file.
     *
     * @param preferenceValues The preferences values to be marshalled
     * @return An XML representing the marshalled value
     * @throws TransformerException If there was an underlying {@link TransformerException}
     * @throws ParserConfigurationException If there was an underlying {@link ParserConfigurationException}
     */
    public static InputStream marshall(final Collection<SystemPreferenceValue> preferenceValues) throws TransformerException, ParserConfigurationException{
        final DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document document = documentBuilder.newDocument();

        final Element preferences = document.createElement("preferences");

        final List<SystemPreferenceValue> sortedPreferences = preferenceValues
                .stream()
                .sorted((pv1, pv2) -> pv1.getName().compareTo(pv2.getName()))
                .collect(Collectors.toList());

        for(final SystemPreferenceValue preferenceValue : sortedPreferences){
            final Element preference = document.createElement("preference");

            final Element preferenceName = document.createElement("preferenceName");
            preferenceName.appendChild(document.createTextNode(preferenceValue.getName()));
            preference.appendChild(preferenceName);

            final Optional<String> optionalValue = preferenceValue.getValue();

            if(optionalValue.isPresent()){
                final Element preferenceValueNode = document.createElement("preferenceValue");
                preferenceValueNode.appendChild(document.createTextNode(optionalValue.get()));
                preference.appendChild(preferenceValueNode);
            }

            preferences.appendChild(preference);
        }

        document.appendChild(preferences);

        return ImportExportUtility.getStreamFromDoc(document);
    }

    /**
     * Unmarshalls an InputStream containing an XML file produced by the marshaller into their system preference values.
     *
     * @param inputStream The stream containing the XML representation
     * @return The system preference values encoded in the input stream
     * @throws SAXException If there was an underlying {@link SAXException}
     * @throws IOException If there was an underlying {@link IOException}
     * @throws ParserConfigurationException If there was an underlying {@link ParserConfigurationException}
     */
    public static Collection<SystemPreferenceValue> unmarshall(final InputStream inputStream)
            throws SAXException, IOException, ParserConfigurationException {
        final Collection<SystemPreferenceValue> preferenceValues = new LinkedList<>();

        final Document document = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new InputStreamReader(inputStream)));

        final NodeList preferenceNodes = document.getElementsByTagName("preference");
        for(int i = 0; i < preferenceNodes.getLength(); i++){
            final Node preferenceNode = preferenceNodes.item(i);
            final String name = preferenceNode.getFirstChild().getFirstChild().getNodeValue();

            final Optional<String> optionalValue = findChildElementNode((Element) preferenceNode, "preferenceValue")
                    .map(node -> node.getFirstChild() == null ? "" : node.getFirstChild().getNodeValue());

            preferenceValues.add(new SystemPreferenceValue(name, optionalValue));
        }

        return preferenceValues;
    }

    /**
     * Takes an XML node and finds a direct child element with a given tag name.
     *
     * @param parent the parent XML node
     * @param tagName the tagname of the nodes to search for
     * @return a list of the matching XML elements
     */
    private static Optional<Element> findChildElementNode(final Element parent, final String tagName){
        Optional<Element> returnValue = Optional.empty();

        final NodeList childNodes = parent.getChildNodes();
        for(int i = 0; i < childNodes.getLength(); i++){
            final Node currentChild = childNodes.item(i);
            if(currentChild.getNodeType() == Node.ELEMENT_NODE
                    && currentChild.getNodeName().equals(tagName)){
                returnValue = Optional.of((Element) currentChild);
                break;
            }
        }

        return returnValue;
    }
}