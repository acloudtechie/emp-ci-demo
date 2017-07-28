package net.micropact.aea.tu.page.objectExport;

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
 * This page generates an XML file with user-configured information surrounding the Template Utility.
 * The XML file is intended to be ingested by the {@link net.micropact.aea.tu.page.objectImport.ObjectImportController}.
 *
 * @author zmiller
 * @see net.micropact.aea.tu.page.objectImport.ObjectImportController
 * @see ImportExportUtility*/
public class ObjectExportController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {
        try {

            final TextResponse response = etk.createTextResponse();

            response.setContentType("text/xml");
            response.setHeader("Content-disposition", "attachment; filename=\"" +"tu_export.xml" +"\"");
            response.put("out", generateXml(etk));

            return response;

        } catch (final TransformerException | ParserConfigurationException e) {
            throw new ApplicationException(e);
        }
    }


    /**
     * Generates an XML which contains all user-configurable data related to the Template Utility.
     *
     * @param etk entellitrak execution context.
     * @return A String which is a valid XML document.
     * @throws TransformerException If there was an underlying {@link TransformerException}.
     * @throws ParserConfigurationException If there was an underlying {@link ParserConfigurationException}.
     */
    private static String generateXml(final ExecutionContext etk)
            throws TransformerException, ParserConfigurationException{

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        final Document document = documentBuilder.newDocument();

        final Element root = document.createElement("objects");
        document.appendChild(root);

        /*T_TU_REPLACEMENT_VARIABLE*/
        ImportExportUtility.addListToXml(document,
                root,
                "T_TU_REPLACEMENT_VARIABLE",
                etk.createSQL(Utility.isSqlServer(etk) ? "SELECT id, c_code, c_description, CONVERT(VARCHAR, etk_end_date, 101) etk_end_date, c_name, c_sql, CONVERT(VARCHAR, etk_start_date, 101) etk_start_date FROM t_tu_replacement_variable"
                        : "SELECT id, c_code, c_description, TO_CHAR(etk_end_date, 'MM/DD/YYYY') etk_end_date, c_name, c_sql, TO_CHAR(etk_start_date, 'MM/DD/YYYY') etk_start_date FROM t_tu_replacement_variable")
                        .fetchList());

        /*T_TU_REPLACEMENT_SECTION*/
        ImportExportUtility.addListToXml(document,
                root,
                "T_TU_REPLACEMENT_SECTION",
                etk.createSQL(Utility.isSqlServer(etk) ? "SELECT id, c_code, CONVERT(VARCHAR, etk_end_date, 101) etk_end_date, c_name, CONVERT(VARCHAR, etk_start_date, 101) etk_start_date, c_text FROM t_tu_replacement_section"
                        : "SELECT id, c_code, TO_CHAR(etk_end_date, 'MM/DD/YYYY') etk_end_date, c_name, TO_CHAR(etk_start_date, 'MM/DD/YYYY') etk_start_date, c_text FROM t_tu_replacement_section")
                        .fetchList());

        return ImportExportUtility.getStringFromDoc(document);
    }
}
