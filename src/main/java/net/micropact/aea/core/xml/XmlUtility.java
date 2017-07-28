package net.micropact.aea.core.xml;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

/**
 * This class contains utility functionality related to dealing with XML files.
 *
 * @author zmiller
 */
public final class XmlUtility {

    /**
     * Utility classes do not need constructors.
     */
    private XmlUtility(){}

    /**
     * This method generates a 'pretty' String representation of an XML file.
     *
     * @param document XML Document to be converted to a String
     * @return The String representation of the XML document
     * @throws TransformerException If there was an underlying {@link TransformerException}
     */
    public static String xmlToString(final Document document) throws TransformerException {
        final Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        final StreamResult result = new StreamResult(new StringWriter());
        final DOMSource source = new DOMSource(document);
        transformer.transform(source, result);
        final String xmlString = result.getWriter().toString();
        return xmlString;
    }
}
