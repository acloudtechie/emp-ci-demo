package net.micropact.aea.du.page.organizationalUnitExport;

import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;

import net.micropact.aea.du.page.organizationalUnitImport.OrganizationalUnitImportController;
import net.micropact.aea.utility.ImportExportUtility;

/**
 * This class serves as the controller code for a page which exports Organizational Unit data from one site to another.
 *
 * @see OrganizationalUnitImportController
 * @author Zachary.Miller
 */
public class OrganizationalUnitExportController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
            return etk.createFileResponse("organizationalUnits.xml", generateXml(etk).getBytes(StandardCharsets.UTF_8));
        } catch (ParserConfigurationException | TransformerException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Generates a String representation of the output data.
     *
     * @param etk entellitrak execution context
     * @return A String representation of the Organizational Unit data
     * @throws ParserConfigurationException If there was an underlying {@link ParserConfigurationException}
     * @throws TransformerException If there was an underlying {@link TransformerException}
     */
    private static String generateXml(final ExecutionContext etk) throws ParserConfigurationException, TransformerException{
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        final Document document = documentBuilder.newDocument();

        final Element root = document.createElement("objects");
        document.appendChild(root);

        /* The ORDER BY clause is currently important to the importer.*/
        ImportExportUtility.addListToXml(document,
                root,
                "ETK_HIERARCHY",
                etk.createSQL("SELECT NODE_ID, PARENT_ID, MAX_CHILD_ID, CODE, NAME FROM etk_hierarchy ORDER BY node_id")
                .fetchList());

        return ImportExportUtility.getStringFromDoc(document);
    }
}
