package net.micropact.aea.eu.page.objectImport;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;

import net.micropact.aea.core.importExport.ComponentDataImporter;
import net.micropact.aea.core.importExport.IImportLogic;
import net.micropact.aea.utility.ImportExportUtility;
import net.micropact.aea.utility.Utility;

/**
 * This page imports configuration data related to the Email Utility.
 * It is intended to ingest files created by {@link net.micropact.aea.eu.page.objectExport.ObjectExportController}.
 * @author zmiller
 * @see net.micropact.aea.eu.page.objectExport.ObjectExportController
 * @see ImportExportUtility
 */
public class ObjectImportController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {

        return ComponentDataImporter.performExecute(etk, new IImportLogic() {

            /**
             * Imports the Eu Email Queue Status reference data.
             *
             * @param document XML document containing user-configurable Email Utility data.
             */
            private void importEmailQueueStatuses(final Document document){
                final List<Map<String, String>> emailQueueStatuses =
                        ImportExportUtility.getTable(document, "T_EU_EMAIL_QUEUE_STATUS");
                for(final Map<String, String> emailQueueStatus : emailQueueStatuses){

                    final List<Map<String, Object>> matchingEmailQueueStatuses = etk.createSQL("SELECT ID FROM t_eu_email_queue_status WHERE c_code = :code")
                            .setParameter("code", emailQueueStatus.get("C_CODE"))
                            .fetchList(); /*ID*/
                    if(matchingEmailQueueStatuses.size() == 0){
                        //Insert
                        etk.createSQL(Utility.isSqlServer(etk)
                                ? "INSERT INTO t_eu_email_queue_status(c_code, c_name) VALUES(:c_code, :c_name)"
                                  : "INSERT INTO t_eu_email_queue_status(id, c_code, c_name) VALUES(OBJECT_ID.NEXTVAL, :c_code, :c_name)")
                                  .setParameter("c_code", emailQueueStatus.get("C_CODE"))
                                  .setParameter("c_name", emailQueueStatus.get("C_NAME"))
                                  .execute();
                    }else{
                        //Update
                        etk.createSQL("UPDATE t_eu_email_queue_status SET c_name = :c_name WHERE id =:id")
                        .setParameter("id", matchingEmailQueueStatuses.get(0).get("ID"))
                        .setParameter("c_name", emailQueueStatus.get("C_NAME"))
                        .execute();
                    }
                }
            }

            /**
             * Imports EU Email Template reference data.
             *
             * @param document XML document containing user-configurable Email Utility data.
             */
            private void importEmailTemplates(final Document document){
                final List<Map<String, String>> emailTemplates =
                        ImportExportUtility.getTable(document, "T_EU_EMAIL_TEMPLATE");
                for(final Map<String, String> emailTemplate : emailTemplates){

                    final List<Map<String, Object>> matchingEmailTemplates = etk.createSQL("SELECT ID FROM t_eu_email_template WHERE c_code = :code")
                            .setParameter("code", emailTemplate.get("C_CODE"))
                            .fetchList(); /*ID*/
                    if(matchingEmailTemplates.size() == 0){
                        //Insert
                        etk.createSQL(Utility.isSqlServer(etk) ? "INSERT INTO t_eu_email_template(c_body, c_code, etk_end_date, etk_start_date, c_subject, c_name) VALUES(:c_body, :c_code, CONVERT(DATE, :etk_end_date, 101), CONVERT(DATE, :etk_start_date, 101), :c_subject, :c_name)"
                                                                 : "INSERT INTO t_eu_email_template(id, c_body, c_code, etk_end_date, etk_start_date, c_subject, c_name) VALUES(OBJECT_ID.NEXTVAL, :c_body, :c_code, TO_DATE(:etk_end_date, 'MM/DD/YYYY'), TO_DATE(:etk_start_date, 'MM/DD/YYYY'), :c_subject, :c_name)")
                             .setParameter("etk_start_date", emailTemplate.get("ETK_START_DATE"))
                             .setParameter("c_body", emailTemplate.get("C_BODY"))
                             .setParameter("c_subject", emailTemplate.get("C_SUBJECT"))
                             .setParameter("c_code", emailTemplate.get("C_CODE"))
                             .setParameter("etk_end_date", emailTemplate.get("ETK_END_DATE"))
                             .setParameter("c_name", emailTemplate.get("C_NAME"))
                             .execute();
                    }else{
                        //Update
                        etk.createSQL(Utility.isSqlServer(etk) ? "UPDATE t_eu_email_template SET c_body = :c_body, c_code = :c_code, etk_end_date = CONVERT(DATE, :etk_end_date, 101), etk_start_date = CONVERT(DATE, :etk_start_date, 101), c_subject = :c_subject, c_name = :c_name WHERE id =:id"
                                                                 : "UPDATE t_eu_email_template SET c_body = :c_body, c_code = :c_code, etk_end_date = TO_DATE(:etk_end_date, 'MM/DD/YYYY'), etk_start_date = TO_DATE(:etk_start_date, 'MM/DD/YYYY'), c_subject = :c_subject, c_name = :c_name WHERE id =:id")
                             .setParameter("id", matchingEmailTemplates.get(0).get("ID"))
                             .setParameter("etk_start_date", emailTemplate.get("ETK_START_DATE"))
                             .setParameter("c_body", emailTemplate.get("C_BODY"))
                             .setParameter("c_subject", emailTemplate.get("C_SUBJECT"))
                             .setParameter("c_code", emailTemplate.get("C_CODE"))
                             .setParameter("etk_end_date", emailTemplate.get("ETK_END_DATE"))
                             .setParameter("c_name", emailTemplate.get("C_NAME"))
                             .execute();
                    }
                }
            }

            /**
             * Imports the AEA CORE Configuration reference data.
             * Eventually the bulk of this method will go into the ImportExportUtility.
             *
             * @param document XML document containing user-configurable Email Utility data.
             */
            private void importAeaCoreConfiguration(final Document document){
                final List<Map<String, String>> aeaCoreConfigurations =
                        ImportExportUtility.getTable(document, "T_AEA_CORE_CONFIGURATION");
                for(final Map<String, String> coreConfiguration : aeaCoreConfigurations){
                    //We have to check that it is email related.
                    // (Note: this could also go in the export portion and if we ever add files it probably should
                    // in order to prevent multiple large export files.
                    if(coreConfiguration.get("C_CODE").startsWith("eu.")){
                        final List<Map<String, Object>> matchingCoreConfigurations = etk.createSQL("SELECT ID FROM t_aea_core_configuration WHERE c_code = :code")
                                .setParameter("code", coreConfiguration.get("C_CODE"))
                                .fetchList(); /*ID*/
                        if(matchingCoreConfigurations.size() == 0){
                            //Insert
                            etk.createSQL(Utility.isSqlServer(etk) ? "INSERT INTO t_aea_core_configuration(c_code, c_description, c_value) VALUES(:c_code, :c_description, :c_value)"
                                                                     : "INSERT INTO t_aea_core_configuration(id, c_code, c_description, c_value) VALUES(OBJECT_ID.NEXTVAL, :c_code, :c_description, :c_value)")
                                 .setParameter("c_code", coreConfiguration.get("C_CODE"))
                                 .setParameter("c_description", coreConfiguration.get("C_DESCRIPTION"))
                                 .setParameter("c_value", coreConfiguration.get("C_VALUE"))
                                 .execute();
                        }else{
                            //Update
                            etk.createSQL("UPDATE t_aea_core_configuration SET c_code = :c_code, c_description = :c_description, c_value = :c_value WHERE id =:id")
                            .setParameter("id", matchingCoreConfigurations.get(0).get("ID"))
                            .setParameter("c_code", coreConfiguration.get("C_CODE"))
                            .setParameter("c_description", coreConfiguration.get("C_DESCRIPTION"))
                            .setParameter("c_value", coreConfiguration.get("C_VALUE"))
                            .execute();
                        }

                    }

                }
            }

            @Override
            public void performImport(final InputStream inputStream)
                    throws ApplicationException{
                try {
                    final Document document = DocumentBuilderFactory
                            .newInstance()
                            .newDocumentBuilder()
                            .parse(new InputSource(new InputStreamReader(inputStream)));

                    importEmailQueueStatuses(document);
                    importEmailTemplates(document);
                    importAeaCoreConfiguration(document);
                } catch (final SAXException | IOException | ParserConfigurationException e) {
                    throw new ApplicationException(e);
                }
            }
        });
    }
}
