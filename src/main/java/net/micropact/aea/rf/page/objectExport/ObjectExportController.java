package net.micropact.aea.rf.page.objectExport;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.query.Coersion;
import net.micropact.aea.rf.service.RfExportLogic;
import net.micropact.aea.utility.ImportExportUtility;

/**
 * This page generates an XML representation of user-entered data surrounding the Rules Framework.
 * The XML file is intended to be ingested by the {@link net.micropact.aea.rf.page.objectImport.ObjectImportController}
 *
 * @author zmiller
 * @see net.micropact.aea.rf.page.objectImport.ObjectImportController
 * @see ImportExportUtility
 *  */
public class ObjectExportController implements PageController{

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();

            /*Our 2 requested actions are going to be initial and generateXml,
             * we're not really going to worry about error handling in this page.*/
            final String requestedAction = Optional.ofNullable(etk.getParameters().getSingle("requestedAction")).orElse("initial");

            final String form; //form will either be Initial, or serveXml

            /*Here is where we actually determine whether this is the first time viewing the page,
             * or whether they are submitting the form*/
            switch(requestedAction){
                case "initial":
                    response.put("rfWorkflows", etk.createSQL("SELECT rf.ID, rf.C_CODE FROM t_rf_workflow rf ORDER BY rf.c_code")
                            .fetchJSON());
                    form = "initial";
                    break;
                case "generateXml":
                    form = "serveXml";
                    final List<String> rfWorkflowInputIds = etk.getParameters().getField("rfWorkflowIds") != null
                            ? etk.getParameters().getField("rfWorkflowIds")
                            : Collections.emptyList();

                            final List<Long> rfWorkflowIds = Coersion.toLongs(rfWorkflowInputIds);

                            response.setContentType("text/xml");
                            response.setHeader("Content-disposition", "attachment; filename=\"" +"rf_export.xml" +"\"");
                            response.put("xml", ImportExportUtility.getStringFromDoc(
                                    RfExportLogic.generateXml(etk, rfWorkflowIds)));
                            break;
                default:
                    throw new ApplicationException("requestedAction was not \"initial\" or \"generateXml\".");
            }

            response.put("form", form);
            return response;

        } catch (final ParserConfigurationException | TransformerException e) {
            throw new ApplicationException(e);
        }
    }
}
