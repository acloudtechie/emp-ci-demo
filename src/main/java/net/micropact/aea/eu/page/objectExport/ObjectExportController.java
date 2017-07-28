package net.micropact.aea.eu.page.objectExport;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

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
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.ImportExportUtility;
import net.micropact.aea.utility.Utility;

/**
 * This page creates an XML representation of the configuration data surrounding the Email Utility.
 * It is intended to be used in conjunction with the
 * {@link net.micropact.aea.eu.page.objectImport.ObjectImportController} which is capable of ingesting the XML file.
 *
 * @author zmiller
 * @see net.micropact.aea.eu.page.objectImport.ObjectImportController
 * @see ImportExportUtility
 */
public class ObjectExportController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {
        try {

            final TextResponse response = etk.createTextResponse();

            /*Our 2 requested actions are going to be initial and generateXml,
             * we're not really going to worry about error handling in this page.*/
            final String requestedAction = Optional.ofNullable(etk.getParameters().getSingle("requestedAction"))
                    .orElse("initial");
            final String form; //form will either be Initial, or serveXml

            /*Here is where we actually determine whether this is the first time viewing the page,
             * or whether they are submitting the form*/

            switch(requestedAction){
                case "initial":
                    form = "initial";
                    break;
                case "generateXml":
                    form = "serveXml";
                    final boolean includeTemplates = "1".equals(etk.getParameters().getSingle("includeTemplates"));
                    final boolean includeCoreConfiguration = "1".equals(etk.getParameters().getSingle("includeCoreConfiguration"));

                    response.setContentType("text/xml");
                    response.setHeader("Content-disposition", "attachment; filename=\"" +"eu_export.xml" +"\"");
                    response.put("xml", generateXml(etk, includeTemplates, includeCoreConfiguration));
                    break;
                default:
                    throw new ApplicationException("requestedAction was not \"initial\" or \"generateXml\".");
            }

            response.put("form", form);
            return response;
        } catch (final Exception e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Generates an XML file containing all user-configurable data related to the Email Utility.
     *
     * @param etk entellitrak execution context
     * @param includeTemplates Whether to include EU Email Templates in the export
     * @param includeCoreConfiguration Whether to include AEA Core Configuration values in the export
     * @return A String which is an XML document
     * @throws TransformerException If there was an underlying {@link TransformerException}
     * @throws ParserConfigurationException If there was an underlying {@link ParserConfigurationException}
     */
    private static String generateXml(final ExecutionContext etk,
            final boolean includeTemplates,
            final boolean includeCoreConfiguration)
                    throws TransformerException, ParserConfigurationException{

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        final Document document = documentBuilder.newDocument();

        final Element root = document.createElement("objects");
        document.appendChild(root);

        /*T_EU_EMAIL_QUEUE_STATUS*/
        ImportExportUtility.addListToXml(document,
                root,
                "T_EU_EMAIL_QUEUE_STATUS",
                etk.createSQL("SELECT * FROM t_eu_email_queue_status")
                .fetchList());

        /*T_EU_EMAIL_TEMPLATE*/
        ImportExportUtility.addListToXml(document,
                root,
                "T_EU_EMAIL_TEMPLATE",
                includeTemplates ?
                etk.createSQL(Utility.isSqlServer(etk) ? "SELECT id, c_body, c_code, CONVERT(VARCHAR, etk_end_date, 101) etk_end_date, CONVERT(VARCHAR, etk_start_date, 101) etk_start_date, c_subject, c_name FROM t_eu_email_template"
                                                         : "SELECT id, c_body, c_code, TO_CHAR(etk_end_date, 'MM/DD/YYYY') etk_end_date, TO_CHAR(etk_start_date, 'MM/DD/YYYY') etk_start_date, c_subject, c_name FROM t_eu_email_template")
                .fetchList()
                : Collections.<Map<String, Object>>emptyList());

        /* T_AEA_CORE_CONFIGURATION */
        // This piece will eventually go into the import/export utility as it will be used from multiple places,
        ImportExportUtility.addListToXml(document,
                root,
                "T_AEA_CORE_CONFIGURATION",
                includeCoreConfiguration
                ?
                etk.createSQL("SELECT * FROM t_aea_core_configuration")
                .fetchList()
                : Collections.<Map<String, Object>>emptyList());

        return ImportExportUtility.getStringFromDoc(document);
    }
}
