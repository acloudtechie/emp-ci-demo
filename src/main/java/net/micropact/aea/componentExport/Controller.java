package net.micropact.aea.componentExport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.entellitrak.ApplicationException;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.SQLFacade;
import com.entellitrak.legacy.util.StringUtility;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Parameters;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.componentImport.Controller.AccessLevel;
import net.micropact.aea.componentImport.Controller.LanguageType;
import net.micropact.aea.core.ioUtility.Base64;
import net.micropact.aea.core.ioUtility.IOUtility;

/**
*
* This page will be used to create a custom XML representation of part of the tracking configuration. It will allow you to specify which BTOs and Script Objects you wish to export.
* All of the hard work is going to be left to the importer.
* This page is used in conjunction with AEA - Page - Object Import
*
* alee 08/29/2014 - Convert to Java / 3.15
**/
public class Controller implements PageController {

    private BigDecimal nextTrackingConfigId = null;
    private BigDecimal workspaceId = null;
    private PageExecutionContext etk;

    @Override
    public Response execute(final PageExecutionContext theEtk) throws ApplicationException {
        this.etk = theEtk;

        Response finalResponse = theEtk.createTextResponse();

        try {
            Integer viewCount = 0;

            if (isSqlServer()) {
                viewCount = etk.createSQL("select count(*) from sys.views where name = 'AEA_SCRIPT_PKG_VIEW_SYS_ONLY'").fetchInt();
            } else {
                viewCount = etk.createSQL("select count(*) from user_views where view_name = 'AEA_SCRIPT_PKG_VIEW_SYS_ONLY'").fetchInt();
            }

            if (0 == viewCount) {
            	final TextResponse response = theEtk.createTextResponse();
                response.put("errorMessage", "Please install and run page \"AEA - DbUtils - Installer\" prior to running "
                        + "this tool. The installation of database views is required for this utility to operate.");

                response.setContentType(ContentType.HTML);
                response.put("form", "errorForm");
                return response;
            }

        } catch (final Exception e) {
            throw new ApplicationException("Could not verify AEA_SCRIPT_PKG_VIEW_SYS_ONLY view's existance, quitting.", e);
        }

        try {
            /* This code will create an XML representation of a tracking configuration.
             * It will only export the LATEST (yet to be deployed) tracking configuration.
             * It should be pretty obvious what's happening but each table will be an
             * element whose tag is the table name, it will have a bunch of rows
             * underneath and an entry will be a tag that is the column name.
             */
            nextTrackingConfigId = (BigDecimal) theEtk.createSQL("SELECT MAX(trackingConfig.tracking_config_id) TRACKINGCONFIGID "
                    + "FROM etk_tracking_config trackingConfig WHERE trackingConfig.config_version = "
                    + "(SELECT MAX(innerTrackingConfig.config_version) FROM etk_tracking_config innerTrackingConfig)")
                    .fetchObject();

            workspaceId = (BigDecimal) theEtk.createSQL("select workspace_id from etk_workspace where workspace_name = 'system' and user_id is null")
                    .fetchObject();

            finalResponse = exportObjects();
        } catch (final Exception e) {
            throw new ApplicationException(e);
        }

        return finalResponse;
    }

    /**
     * Converts an XML document to a prettified String representation.
     *
     * @param doc XML document
     * @return a string representation of the XML document
     * @throws TransformerException If there was an underlying {@link TransformerException}
     */
    private static String getStringFromDoc(final Document doc) throws TransformerException {
        StringWriter writer = null;
        try{
            final DOMSource domSource = new DOMSource(doc);
            writer = new StringWriter();
            final StreamResult result = new StreamResult(writer);
            final TransformerFactory tf = TransformerFactory.newInstance();

            final Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(domSource, result);
            writer.flush();

            return writer.toString();
        }finally{
            IOUtility.closeQuietly(writer);
        }
    }

    /**
     * This method appends a new XML node (with the name specified by tagName) and text content given by value to
     * parentNode.
     *
     * @param document XML document which the node will be attached to
     * @param parentNode node to append the new node to
     * @param tagName name of the new XML tag
     * @param value the value contained within the new tag
     */
    private static void addSimpleElement(final Document document, final Element parentNode, final String tagName, final Object value){
        final Element element = document.createElement(tagName);

        if (value == null) {
        	element.appendChild(document.createTextNode(""));
        } else if (value instanceof BigDecimal) {
        	final BigDecimal bdVal = (BigDecimal) value;

        	try {
        		element.appendChild(document.createTextNode(bdVal.longValueExact() + ""));
        	} catch (final ArithmeticException ae) {
        		//Use decimal logic.
        		element.appendChild(document.createTextNode(bdVal.toPlainString()));
        	}
        } else if (value instanceof byte[]) {
        	element.appendChild(document.createTextNode(new String(Base64.encodeBase64((byte[]) value))));
        } else {
        	element.appendChild(document.createTextNode(value.toString()));
        }

        parentNode.appendChild(element);
    }

    /**
     * Adds a new XML file with information about one of the entellitrak tables to the zip file.
     *
     * @param outputStream the zip file's output stream
     * @param subFolder the location within the zip file to store the XML file
     * @param groupName the name to use for both the file and the root XML element
     * @param rows the rows containing the table-data represented by the XML file
     * @throws IOException If there was an underlying {@link IOException}
     * @throws ParserConfigurationException If there was an underlying {@link ParserConfigurationException}
     * @throws TransformerException If there was an underlying {@link TransformerException}
     */
    private static void addListToXml(final ZipOutputStream outputStream,
            final String subFolder,
            final String groupName,
            final List<Map<String, Object>> rows)
    		   throws IOException, ParserConfigurationException, TransformerException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        final Document document = documentBuilder.newDocument();
        // final Element root = document.createElement("tracking-config");
        // document.appendChild(root);

        final Element root = document.createElement(groupName);
        document.appendChild(root);

        for(final Map<String, Object> row : rows){

            //Maintain ordering of columns.
            final List<String> keyList = new ArrayList<>(row.keySet());
            Collections.sort(keyList);

            final Element element = document.createElement("row");
            root.appendChild(element);
            for(final String key : keyList){
                addSimpleElement(document, element, key, row.get(key));
            }
        }

        outputStream.putNextEntry(new ZipEntry(subFolder + "/" + groupName + ".xml"));
        outputStream.write(getStringFromDoc(document).getBytes());
        outputStream.closeEntry();
    }

    /**
     * Method to return associated scripts for dataObjects, pages and scheduler jobs.
     *
     * @param dataObjectIds IDs of data objects.
     * @param etkPageObjectIds IDs of page objects.
     * @param jobBusinessKeys business keys of the scheduler jobs.
     * @return IDs of script objects.
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private List<BigDecimal> getAssociatedScripts (final Set<BigDecimal> dataObjectIds,
            final Set<BigDecimal> etkPageObjectIds,
            final List<String> jobBusinessKeys)
                    throws IncorrectResultSizeDataAccessException {

        final List<BigDecimal> idList = new ArrayList<>();

        //Add scripts for data objects.
        if ((dataObjectIds != null) && (dataObjectIds.size() > 0)) {
            final StringBuilder query = new StringBuilder();

            final List<BigDecimal> dataObjectIdList = new ArrayList<>();
            dataObjectIdList.addAll(dataObjectIds);

            final Map<String, Object> paramMap = new HashMap<>();

            /* Display Mapping Evaluation Scripts */
            query.append(" SELECT  ");
            query.append(" displayMapping.EVALUATION_SCRIPT_ID as SCRIPT_ID ");
            query.append(" FROM etk_data_object do  ");
            query.append(" JOIN etk_display_mapping displayMapping ON displayMapping.data_object_id = do.data_object_id  ");
            query.append(" WHERE ");
            addLargeInClause("do.data_object_id", query, paramMap, dataObjectIdList);
            query.append(" AND displayMapping.EVALUATION_SCRIPT_ID is not null ");

            query.append(" UNION ALL ");

            /* ETP Transition Scripts */
            query.append(" SELECT doTransition.TRIGGER_SCRIPT_ID as SCRIPT_ID  FROM etk_data_object DO  ");
            query.append(" JOIN etk_do_state doState ON doState.data_object_id = do.data_object_id  ");
            query.append(" JOIN etk_do_transition doTransition ON doTransition.do_previous_state_id = doState.do_state_id  ");
            query.append(" WHERE ");
            addLargeInClause("do.data_object_id", query, paramMap, dataObjectIdList);
            query.append(" AND doTransition.TRIGGER_SCRIPT_ID is not null ");

            query.append(" UNION ALL ");

            /* Data Form Event Handler */
            query.append(" SELECT eventHandler.SCRIPT_OBJECT_ID as SCRIPT_ID FROM etk_data_object do ");
            query.append(" JOIN etk_data_form dataForm ON dataForm.data_object_id = do.data_object_id  ");
            query.append(" JOIN etk_data_form_event_handler eventHandler ON eventHandler.data_form_id = dataForm.data_form_id  ");
            query.append(" WHERE do.tracking_config_id = :trackingConfigId AND ");
            addLargeInClause("do.data_object_id", query, paramMap, dataObjectIdList);
            query.append(" AND eventHandler.SCRIPT_OBJECT_ID is not null ");

            query.append(" UNION ALL ");

            /* Data Event Listener */
            query.append(" SELECT dataEventListener.script_object_id SCRIPT_ID ");
            query.append(" FROM etk_data_event_listener dataEventListener ");
            query.append(" WHERE ");
            addLargeInClause("dataEventListener.data_object_id", query, paramMap, dataObjectIdList);

            query.append(" UNION ALL ");


            /* FILTER HANDLER */
            query.append(" SELECT filterHandler.script_object_id SCRIPT_ID ");
            query.append(" FROM etk_filter_handler filterHandler ");
            query.append(" WHERE ");
            addLargeInClause("filterHandler.data_object_id", query, paramMap, dataObjectIdList);

            query.append(" UNION ALL ");

            /* Form Control Event */
            query.append(" SELECT formControlEventHandler.SCRIPT_OBJECT_ID as SCRIPT_ID FROM etk_data_object do  ");
            query.append(" JOIN etk_data_form dataForm ON dataForm.data_object_id = do.data_object_id  ");
            query.append(" JOIN etk_form_control formControl ON formControl.data_form_id = dataForm.data_form_id ");
            query.append(" JOIN etk_form_control_event_handler formControlEventHandler ON formControlEventHandler.form_control_id = formControl.form_control_id  ");
            query.append(" WHERE do.tracking_config_id = :trackingConfigId AND ");
            addLargeInClause("do.data_object_id", query, paramMap, dataObjectIdList);
            query.append(" AND formControlEventHandler.SCRIPT_OBJECT_ID is not null ");

            query.append(" UNION ALL ");

            /* Form Evaluation Script (Responsive Script)*/
            query.append(" SELECT dataForm.script_object_id AS SCRIPT_ID FROM etk_data_form dataForm WHERE ");
            addLargeInClause("dataForm.data_object_id", query, paramMap, dataObjectIdList);
            query.append(" AND dataForm.script_object_id IS NOT NULL ");

            query.append(" union all ");

            /* Lookup */
            query.append(" SELECT lookup.SQL_SCRIPT_OBJECT_ID as SCRIPT_ID FROM etk_lookup_definition lookup  ");
            query.append(" WHERE (EXISTS  ");
            query.append(" ( ");
            query.append("     SELECT * FROM etk_data_element de  ");
            query.append("     WHERE ");
            addLargeInClause("de.data_object_id", query, paramMap, dataObjectIdList);
            query.append("     AND de.lookup_definition_id = lookup.lookup_definition_id  ");
            query.append(" )  ");
            query.append("   OR EXISTS ");
            query.append(" ( ");
            query.append("     SELECT * FROM etk_data_form df  ");
            query.append("     JOIN etk_form_control fc ON fc.data_form_id = df.data_form_id  ");
            query.append("     JOIN etk_form_ctl_lookup_binding fclb ON fclb.form_control_id = fc.form_control_id  ");
            query.append("     WHERE ");
            addLargeInClause("df.data_object_id", query, paramMap, dataObjectIdList);
            query.append("     AND fclb.lookup_definition_id = lookup.lookup_definition_id  ");
            query.append(" )) ");
            query.append(" AND lookup.SQL_SCRIPT_OBJECT_ID is not null ");



            final List<Map<String, Object>> queryResult =
                    etk.createSQL(query.toString())
                    .setParameter("trackingConfigId", nextTrackingConfigId)
                    .setParameter(paramMap)
                    .returnEmptyResultSetAs(new ArrayList<Map<String, Object>>())
                    .fetchList();

            for (final Map<String, Object> aResult : queryResult) {
                idList.add((BigDecimal) aResult.get("SCRIPT_ID"));
            }
        }

        //Add scripts for pages
        if ((etkPageObjectIds != null) && (etkPageObjectIds.size() > 0)) {

            final SQLFacade pageScriptPS = etk.createSQL(" SELECT page.CONTROLLER_SCRIPT_ID as CONTROLLER_SCRIPT_ID, "
                    + " page.VIEW_SCRIPT_ID as VIEW_SCRIPT_ID FROM etk_page page "
                    + " WHERE page.PAGE_ID = :pageId");
            Map<String, Object> scriptIdMap;
            for(final BigDecimal etkPageObjectId : etkPageObjectIds) {

                scriptIdMap = pageScriptPS
                        .setParameter("workspaceId", workspaceId)
                        .setParameter("trackingConfigId", nextTrackingConfigId)
                        .setParameter("pageId", etkPageObjectId)
                        .returnEmptyResultSetAs(null)
                        .fetchMap();

                if (scriptIdMap != null) {

                    if (scriptIdMap.get("CONTROLLER_SCRIPT_ID") != null) {
                        idList.add((BigDecimal) scriptIdMap.get("CONTROLLER_SCRIPT_ID"));
                    } else {
                        etk.getLogger().error("ERROR - AEA Component Export - etk_page with ID " + etkPageObjectId
                                + " does not have an associated CONTROLLER_SCRIPT_ID value.");
                    }

                    if (scriptIdMap.get("VIEW_SCRIPT_ID") != null) {
                        idList.add((BigDecimal) scriptIdMap.get("VIEW_SCRIPT_ID"));
                    } else {
                        etk.getLogger().error("WARNING - AEA Component Export - etk_page with ID " + etkPageObjectId
                                + " does not have an associated VIEW_SCRIPT_ID value, possibly mobile page.");
                    }
                }
            }
        }

        final Map<String, Object> paramMap = new HashMap<>();
        final StringBuilder query = new StringBuilder();
        query.append(" select jc.script_object_id as JOB_SCRIPT_ID FROM ETK_JOB ej ");
        query.append(" join ETK_JOB_CUSTOM jc on jc.job_custom_id = ej.job_id where ");
        addLargeInClause("ej.BUSINESS_KEY", query, paramMap, jobBusinessKeys);

        /* Add scripts related to custom scheduler jobs */
        if (jobBusinessKeys != null && jobBusinessKeys.size() > 0) {
            final List<Map<String, Object>> scriptIdList =
                    etk.createSQL(query.toString())
                    .setParameter(paramMap)
                    .returnEmptyResultSetAs(null)
                    .fetchList();

            if (scriptIdList != null && scriptIdList.size() > 0) {

                for (final Map<String, Object> scriptId : scriptIdList) {
                    idList.add((BigDecimal) scriptId.get("JOB_SCRIPT_ID"));
                }
            }
        }

        return idList;
    }

    /**
     * Generates the zip file. It takes as parameters the items which should be contained in the zip file.
     *
     * @param dataObjectIdList data object ids of the BTOs, SDOs, RDOs to export in the zip file
     * @param scriptObjectIds the script object ids of the scripts to export
     * @param etkPageObjectIds the ids of pages to export
     * @param roleBusinessKeys the busienss keys of roles to export
     * @param groupBusinessKeys the business keys of groups to export
     * @param jobBusinessKeys the business keys of jobs to export
     * @param selectedComponentCCodes the pre-packaged components to export
     * @param outputStream the stream to write the zip file to
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     * @throws IOException If there was an underlying {@link IOException}
     * @throws ParserConfigurationException If there was an underlying {@link ParserConfigurationException}
     * @throws TransformerException If there was an underlying {@link TransformerException}
     */
    private void generateXml(final Set<BigDecimal> dataObjectIdList,
            final Set<BigDecimal> scriptObjectIds,
            final Set<BigDecimal> etkPageObjectIds,
            final List<String> roleBusinessKeys,
            final List<String> groupBusinessKeys,
            final List<String> jobBusinessKeys,
            final List<String> selectedComponentCCodes,
            final ZipOutputStream outputStream) throws IncorrectResultSizeDataAccessException,
                                                       IOException,
                                                       ParserConfigurationException,
                                                       TransformerException {

    	StringBuilder query = new StringBuilder();
    	Map<String, Object> scriptObjectParamMap = new HashMap<>();

        final Set <BigDecimal> dataObjectIds = new TreeSet<>();
        dataObjectIds.addAll(dataObjectIdList);
        /*Converts an XML Document to its "normal" string representation*/
        /* When we enter this loop, dataObjectIds just has BTOs and reference data.
         * We're going to add all the descendants of the BTOs now. */
        for(final BigDecimal aDataObjectId : dataObjectIdList) {
            final List<Map<String, Object>> currentChildrenMaps = etk.createSQL(
                    "with related_objects (data_object_id) as (" +
                            " SELECT data_object_id FROM etk_data_object do WHERE do.parent_object_id = :parentObjectId " +
                            "  union all " +
                            " SELECT nplus1.data_object_id from etk_data_object nplus1, related_objects " +
                            " where related_objects.data_object_id = nplus1.parent_object_id " +
                    ") select data_object_id from related_objects ")
                    .setParameter("parentObjectId", aDataObjectId)
                    .fetchList();
            for(final Map<String, Object> currentChild : currentChildrenMaps) {
                dataObjectIds.add((BigDecimal) currentChild.get("DATA_OBJECT_ID"));
            }
        }

        //Find and add all associated script object IDs that are associated with dataObjectIds
        //ETK_DISPLAY_MAPPING.EVALUATION_SCRIPT_ID
        //ETK_DATA_FORM_EVENT_HANDLER.SCRIPT_OBJECT_ID
        //ETK_FORM_CONTROL_EVENT_HANDLER.SCRIPT_OBJECT_ID
        //ETK_LOOKUP_DEFINITION.SQL_SCRIPT_OBJECT_ID
        //ETK_DO_TRANSITION.TRIGGER_SCRIPT_ID


        //Automatically adds scripts that are associated with data objects and pages to the export.
        scriptObjectIds.addAll(getAssociatedScripts(dataObjectIds, etkPageObjectIds, jobBusinessKeys));


        query.append("SELECT * FROM T_AEA_COMPONENT_VERSION_INFO WHERE ");
        addLargeInClause("C_CODE", query, scriptObjectParamMap, selectedComponentCCodes);
        query.append(" order by C_CODE");


        addListToXml(outputStream,
                "ETK_TABLES",
                "T_AEA_COMPONENT_VERSION_INFO",
                selectedComponentCCodes.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /*ETK_SCRIPT_OBJECT*/
        /* This query could actually do a little more, such as look at script objects which are set as form_event_handlers and the like. */

        final List<BigDecimal> scriptObjectList = new ArrayList<>();
        scriptObjectList.addAll(scriptObjectIds);

        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append (" SELECT so.* FROM etk_script_object so WHERE so.tracking_config_id = :trackingConfigId ");
        query.append (" AND so.workspace_id = :workspaceId ");
        query.append (" AND ");
        addLargeInClause("so.script_id", query, scriptObjectParamMap, scriptObjectList);
        query.append (" ORDER BY so.BUSINESS_KEY ");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_SCRIPT_OBJECT",
                scriptObjectIds.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                        .setParameter("workspaceId", workspaceId)
                        .setParameter("trackingConfigId", nextTrackingConfigId)
                        .setParameter(scriptObjectParamMap)
                        .fetchList());

        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();

        query.append("select SCRIPT_ID, ");
        query.append("       SCRIPT_NAME as SCRIPT_OBJECT_NAME, ");
        query.append("       PACKAGE_NODE_ID, ");
        query.append("       PACKAGE_TYPE, ");
        query.append("       PACKAGE_PATH from AEA_SCRIPT_PKG_VIEW_SYS_ONLY ");
        query.append(" WHERE ");
        addLargeInClause("script_id", query, scriptObjectParamMap, scriptObjectList);
        query.append(" order by PACKAGE_PATH, SCRIPT_NAME");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_PACKAGE_INFO",
                scriptObjectIds.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        final List<BigDecimal> finalDataObjectIdList = new ArrayList<>();
        finalDataObjectIdList.addAll(dataObjectIds);


        /*ETK_TRACKING_EVENT_LISTENER*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("SELECT trackingEventListener.* FROM etk_data_object do ");
        query.append("JOIN etk_tracking_event_listener trackingEventListener ON trackingEventListener.data_object_id = do.data_object_id ");
        query.append("WHERE do.tracking_config_id = :trackingConfigId ");
        query.append("AND ");
        addLargeInClause("do.data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append(" ORDER BY trackingEventListener.BUSINESS_KEY, do.BUSINESS_KEY");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_TRACKING_EVENT_LISTENER",
                finalDataObjectIdList.isEmpty()
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                        .setParameter("trackingConfigId", nextTrackingConfigId)
                        .setParameter(scriptObjectParamMap)
                        .fetchList());

        /*ETK_DATA_OBJECT*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();

        query.append("SELECT * FROM etk_data_object do ");
        query.append("WHERE do.tracking_config_id = :trackingConfigId AND ");
        addLargeInClause("do.data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append(" ORDER BY do.BUSINESS_KEY");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_DATA_OBJECT",
                finalDataObjectIdList.isEmpty()
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                        .setParameter("trackingConfigId", nextTrackingConfigId)
                        .setParameter(scriptObjectParamMap)
                        .fetchList());

        /*ETK_DATA_ELEMENT*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();

        query.append("SELECT de.* FROM etk_data_object do ");
        query.append("JOIN etk_data_element de ON de.data_object_id = do.data_object_id ");
        query.append("WHERE do.tracking_config_id = :trackingConfigId AND ");
        addLargeInClause("do.data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append("ORDER BY de.BUSINESS_KEY ");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_DATA_ELEMENT",
                finalDataObjectIdList.isEmpty()
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                        .setParameter("trackingConfigId", nextTrackingConfigId)
                        .setParameter(scriptObjectParamMap)
                        .fetchList());

        /*ETK_DATA_FORM*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();

        query.append("SELECT dataForm.* FROM etk_data_object do ");
        query.append("JOIN etk_data_form dataForm ON dataForm.data_object_id  = do.data_object_id ");
        query.append("WHERE do.tracking_config_id = :trackingConfigId AND ");
        addLargeInClause("do.data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append("ORDER BY dataForm.BUSINESS_KEY, do.BUSINESS_KEY");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_DATA_FORM",
                finalDataObjectIdList.isEmpty()
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                        .setParameter("trackingConfigId", nextTrackingConfigId)
                        .setParameter(scriptObjectParamMap)
                        .fetchList());

        /*ETK_LOOKUP_DEFINITION*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();

        /* Lookup definitions are actually going to reference object/elements by business key instead of by id.
         * This is so that sales can do something they want to do (export objects without needing to exporting the
         * objects needed for data object lookup dependencies). If professional services relies on this feature, it
         * likely means that they need to reconsider how they have planned out their components. */

        query.append("SELECT lookup.LOOKUP_DEFINITION_ID, lookup.LOOKUP_SOURCE_TYPE, dataObject.business_key DATA_OBJECT_ID, valueElement.business_key VALUE_ELEMENT_ID, displayElement.business_key DISPLAY_ELEMENT_ID, orderByElement.business_key ORDER_BY_ELEMENT_ID, lookup.ASCENDING_ORDER, startDateElement.business_key START_DATE_ELEMENT_ID, endDateElement.business_key END_DATE_ELEMENT_ID, lookup.SQL_SCRIPT_OBJECT_ID, lookup.PLUGIN_REGISTRATION_ID, lookup.VALUE_RETURN_TYPE, lookup.TRACKING_CONFIG_ID, lookup.BUSINESS_KEY, lookup.NAME, lookup.DESCRIPTION, lookup.SYSTEM_OBJECT_TYPE, lookup.SYSTEM_OBJECT_DISPLAY_FORMAT, lookup.ENABLE_CACHING FROM etk_lookup_definition lookup LEFT JOIN etk_data_object dataObject ON dataObject.data_object_id = lookup.data_object_id LEFT JOIN etk_data_element valueElement ON valueElement.data_element_id = lookup.value_element_id LEFT JOIN etk_data_element displayElement ON displayElement.data_element_id = lookup.display_element_id LEFT JOIN etk_data_element orderByElement ON orderByElement.data_element_id = lookup.order_by_element_id LEFT JOIN etk_data_element startDateElement ON startDateElement.data_element_id = lookup.start_date_element_id LEFT JOIN etk_data_element endDateElement ON endDateElement.data_element_id = lookup.end_date_element_id ");
        query.append("WHERE EXISTS (SELECT * FROM etk_data_element de ");
        query.append("WHERE ");
        addLargeInClause("de.data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append(" AND de.lookup_definition_id = lookup.lookup_definition_id ) ");
        query.append("OR EXISTS( SELECT * FROM etk_data_form df ");
        query.append("JOIN etk_form_control fc ON fc.data_form_id = df.data_form_id ");
        query.append("JOIN etk_form_ctl_lookup_binding fclb ON fclb.form_control_id = fc.form_control_id ");
        query.append("WHERE ");
        addLargeInClause("df.data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append("AND fclb.lookup_definition_id = lookup.lookup_definition_id ) ");
        query.append("ORDER BY lookup.BUSINESS_KEY");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_LOOKUP_DEFINITION",
                finalDataObjectIdList.isEmpty()
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                        .setParameter(scriptObjectParamMap)
                        .fetchList());

        /*ETK_DATA_FORM_EVENT_HANDLER*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();

        query.append("SELECT eventHandler.* FROM etk_data_object do ");
        query.append("JOIN etk_data_form dataForm ON dataForm.data_object_id = do.data_object_id ");
        query.append("JOIN etk_data_form_event_handler eventHandler ON eventHandler.data_form_id = dataForm.data_form_id ");
        query.append("WHERE do.tracking_config_id = :trackingConfigId AND ");
        addLargeInClause("do.data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append(" ORDER BY eventHandler.BUSINESS_KEY");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_DATA_FORM_EVENT_HANDLER",
                finalDataObjectIdList.isEmpty()
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                        .setParameter("trackingConfigId", nextTrackingConfigId)
                        .setParameter(scriptObjectParamMap)
                        .fetchList());

        /*ETK_FORM_CONTROL*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();

        query.append("SELECT formControl.* FROM etk_data_object do ");
        query.append("JOIN etk_data_form dataForm ON dataForm.data_object_id = do.data_object_id ");
        query.append("JOIN etk_form_control formControl ON formControl.data_form_id = dataForm.data_form_id ");
        query.append("WHERE do.tracking_config_id = :trackingConfigId AND ");
        addLargeInClause("do.data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append(" ORDER BY formControl.BUSINESS_KEY");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_FORM_CONTROL",
                finalDataObjectIdList.isEmpty()
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                        .setParameter("trackingConfigId", nextTrackingConfigId)
                        .setParameter(scriptObjectParamMap)
                        .fetchList());

        /*ETK_FORM_CONTROL_EVENT_HANDLER*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();

        query.append("SELECT formControlEventHandler.* FROM etk_data_object do ");
        query.append("JOIN etk_data_form dataForm ON dataForm.data_object_id = do.data_object_id ");
        query.append("JOIN etk_form_control formControl ON formControl.data_form_id = dataForm.data_form_id ");
        query.append("JOIN etk_form_control_event_handler formControlEventHandler ON formControlEventHandler.form_control_id = formControl.form_control_id ");
        query.append("WHERE do.tracking_config_id = :trackingConfigId AND ");
        addLargeInClause("do.data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append(" ORDER BY formControlEventHandler.BUSINESS_KEY");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_FORM_CONTROL_EVENT_HANDLER",
                finalDataObjectIdList.isEmpty()
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter("trackingConfigId", nextTrackingConfigId)
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /*ETK_FORM_CTL_ELEMENT_BINDING*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();

        query.append("SELECT formCtlElementBinding.* FROM etk_data_object do JOIN etk_data_form dataForm ON dataForm.data_object_id = do.data_object_id JOIN etk_form_control formControl ON formControl.data_form_id = dataForm.data_form_id JOIN etk_form_ctl_element_binding formCtlElementBinding ON formCtlElementBinding.form_control_id = formControl.form_control_id WHERE ");
        addLargeInClause("do.data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append(" ORDER BY formControl.BUSINESS_KEY, formCtlElementBinding.DATA_ELEMENT_ID");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_FORM_CTL_ELEMENT_BINDING",
                finalDataObjectIdList.isEmpty()
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /*ETK_FORM_CTL_LABEL_BINDING*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();

        query.append("SELECT formCtlLabelBinding.* FROM etk_data_object do JOIN etk_data_form dataForm ON dataForm.data_object_id = do.data_object_id JOIN etk_form_control formControl ON formControl.data_form_id = dataForm.data_form_id JOIN etk_form_ctl_label_binding formCtlLabelBinding ON formCtlLabelBinding.form_control_id = formControl.form_control_id WHERE ");
        addLargeInClause("do.data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append(" ORDER BY formControl.BUSINESS_KEY, formCtlLabelBinding.LABEL_CONTROL_ID");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_FORM_CTL_LABEL_BINDING",
                finalDataObjectIdList.isEmpty()
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /*ETK_FORM_CTL_LOOKUP_BINDING*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("SELECT formCtlLookupBinding.* FROM etk_data_object DO JOIN etk_data_form dataForm ON dataForm.data_object_id = do.data_object_id JOIN etk_form_control formControl ON formControl.data_form_id = dataForm.data_form_id JOIN etk_form_ctl_lookup_binding formCtlLookupBinding ON formCtlLookupBinding.form_control_id = formControl.form_control_id WHERE ");
        addLargeInClause("do.data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append(" ORDER BY formControl.BUSINESS_KEY, formCtlLookupBinding.LOOKUP_DEFINITION_ID");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_FORM_CTL_LOOKUP_BINDING",
                finalDataObjectIdList.isEmpty()
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /*ETK_DATA_VIEW*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("SELECT dataView.* FROM etk_data_object do JOIN etk_data_view dataView ON dataView.data_object_id  = do.data_object_id WHERE ");
        addLargeInClause("do.data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append(" ORDER BY dataView.BUSINESS_KEY");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_DATA_VIEW",
                finalDataObjectIdList.isEmpty()
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /*ETK_DATA_VIEW_ELEMENT*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("SELECT viewElement.* FROM etk_data_object do JOIN etk_data_view dataView ON dataView.data_object_id = do.data_object_id JOIN etk_data_view_element viewElement ON viewElement.data_view_id = dataView.data_view_id WHERE ");
        addLargeInClause("do.data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append(" ORDER BY viewElement.BUSINESS_KEY");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_DATA_VIEW_ELEMENT",
                finalDataObjectIdList.isEmpty()
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /*ETK_DISPLAY_MAPPING*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("SELECT displayMapping.* FROM etk_data_object do JOIN etk_display_mapping displayMapping ON displayMapping.data_object_id = do.data_object_id WHERE ");
        addLargeInClause("do.data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append(" ORDER BY displayMapping.BUSINESS_KEY");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_DISPLAY_MAPPING",
                finalDataObjectIdList.isEmpty()
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /*ETK_DO_STATE*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("SELECT doState.* FROM etk_data_object DO JOIN etk_do_state doState ON doState.data_object_id   = do.data_object_id WHERE ");
        addLargeInClause("do.data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append(" ORDER BY doState.BUSINESS_KEY");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_DO_STATE",
                finalDataObjectIdList.isEmpty()
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());


        /*ETK_DO_TIMER*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("SELECT doTimer.* FROM etk_data_object do JOIN etk_do_state doState ON doState.data_object_id = do.data_object_id JOIN etk_do_timer doTimer ON doTimer.do_state_id = doState.do_state_id WHERE ");
        addLargeInClause("do.data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append(" ORDER BY doTimer.BUSINESS_KEY");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_DO_TIMER",
                finalDataObjectIdList.isEmpty()
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /*ETK_DO_TRANSITION*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("SELECT doTransition.* FROM etk_data_object DO JOIN etk_do_state doState ON doState.data_object_id = do.data_object_id JOIN etk_do_transition doTransition ON doTransition.do_previous_state_id = doState.do_state_id WHERE ");
        addLargeInClause("do.data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append(" ORDER BY doTransition.BUSINESS_KEY");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_DO_TRANSITION",
                finalDataObjectIdList.isEmpty()
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /*ETK_DATA_EVENT_LISTENER*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("SELECT * FROM etk_data_event_listener WHERE ");
        addLargeInClause("data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append(" ORDER BY BUSINESS_KEY");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_DATA_EVENT_LISTENER",
                finalDataObjectIdList.isEmpty()
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /*ETK_FILTER_HANDLER*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("SELECT * FROM etk_filter_handler WHERE ");
        addLargeInClause("data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append(" ORDER BY BUSINESS_KEY");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_FILTER_HANDLER",
                finalDataObjectIdList.isEmpty()
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /*ETK_PLUGIN_REGISTRATION*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();

        query.append("SELECT * FROM etk_plugin_registration pluginRegistration WHERE EXISTS( SELECT * FROM etk_data_object dataObject JOIN etk_data_element dataElement ON dataElement.data_object_id = dataObject.data_object_id WHERE pluginRegistration.plugin_registration_id = dataElement.plugin_registration_id AND ");
        addLargeInClause("dataObject.data_object_id", query, scriptObjectParamMap, finalDataObjectIdList);
        query.append(" ) ORDER BY pluginRegistration.business_key");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_PLUGIN_REGISTRATION",
                finalDataObjectIdList.isEmpty()
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /*ETK_PAGE*/
        final List<BigDecimal> pageIdList = new ArrayList<>();
        pageIdList.addAll(etkPageObjectIds);


        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("SELECT page.* FROM etk_page page WHERE ");
        addLargeInClause("page.page_id", query, scriptObjectParamMap, pageIdList);
        query.append(" ORDER BY page.BUSINESS_KEY");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_PAGE",
                etkPageObjectIds.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /*ETK_PAGE_PERMISSIONS*/
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("select * from etk_shared_object_permission sop ");
        query.append("join etk_page_permission ep on ep.page_permission_id = sop.shared_object_permission_id ");
       	query.append("where ");
       	addLargeInClause("ep.page_id", query, scriptObjectParamMap, pageIdList);
        query.append(" ORDER BY shared_object_permission_id");

        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_PAGE_PERMISSIONS",
                etkPageObjectIds.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                        .setParameter(scriptObjectParamMap)
                        .fetchList());

        /*ETK_ROLE*/
        addListToXml(outputStream,
                "ETK_TABLES",
                "ETK_ROLE",
                etk.createSQL("SELECT * FROM etk_role order by BUSINESS_KEY")
                .fetchList());



        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("SELECT NAME, DESCRIPTION, BUSINESS_KEY, PROFILE FROM etk_role where ");
        addLargeInClause("BUSINESS_KEY", query, scriptObjectParamMap, roleBusinessKeys);
        query.append(" order by BUSINESS_KEY");

        addListToXml(outputStream,
                "ROLES",
                "ROLE",
                roleBusinessKeys.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("SELECT do.name AS DATA_OBJECT_NAME, do.BUSINESS_KEY AS DATA_OBJECT_BUSINESS_KEY, r.name AS ROLE_NAME, r.DESCRIPTION AS ROLE_DESCRIPTION, r.BUSINESS_KEY AS ROLE_BUSINESS_KEY, dp.DATA_OBJECT_TYPE AS DATA_OBJECT_TYPE, dp.DATA_ELEMENT_TYPE AS DATA_ELEMENT_TYPE, dp.CREATE_ACCESS_LEVEL \"CREATE\", dp.READ_ACCESS_LEVEL \"READ\", dp.UPDATE_ACCESS_LEVEL \"UPDATE\", dp.DELETE_ACCESS_LEVEL \"DELETE\", dp.ASSIGN_ACCESS_LEVEL \"ASSIGN\", dp.SEARCHING_ACCESS_LEVEL \"SEARCH\", dp.REPORTING_ACCESS_LEVEL \"REPORTING\", dp.READ_CONTENT_ACCESS_LEVEL \"READ_CONTENT\", dp.INBOX_ENABLED FROM etk_data_permission dp JOIN etk_role r ON r.role_id = dp.role_id JOIN etk_data_object DO ON do.table_name = dp.data_object_type WHERE do.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive ) AND ");
        addLargeInClause("r.BUSINESS_KEY", query, scriptObjectParamMap, roleBusinessKeys);
        query.append(" ORDER BY DATA_OBJECT_NAME, r.name");

        addListToXml(outputStream,
                "ROLES",
                "ROLE_DATA_PERMISSIONS",
                roleBusinessKeys.size() == 0
                ? Collections.emptyList()
                : convertAccessLevelsFromNumberToDisplay(etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList()));

        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("select r.BUSINESS_KEY as BUSINESS_KEY, rp.PERMISSION_KEY as PERMISSION_KEY from ETK_ROLE_PERMISSION rp join ETK_ROLE r on rp.role_id = r.role_id where ");
        addLargeInClause("r.BUSINESS_KEY", query, scriptObjectParamMap, roleBusinessKeys);
        query.append(" order by r.business_key, rp.permission_key");

        addListToXml(outputStream,
                "ROLES",
                "ROLE_PERMISSIONS",
                roleBusinessKeys.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("select ej.*, jc.SCRIPT_OBJECT_ID as SCRIPT_OBJECT_ID, js.job_type as SYSTEM_JOB_TYPE, case when js.job_type is null then 'CUSTOM' else 'SYSTEM' end as JOB_TYPE FROM ETK_JOB ej left join ETK_JOB_CUSTOM jc on jc.job_custom_id = ej.job_id left join ETK_JOB_SYSTEM js on js.job_system_id = ej.job_id where ");
        addLargeInClause("ej.BUSINESS_KEY", query, scriptObjectParamMap, jobBusinessKeys);
        query.append(" order by ej.business_key");

        addListToXml(outputStream,
                "JOBS",
                "JOB_DATA",
                jobBusinessKeys.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());



        /* ETK_QRTZ_TRIGGERS */
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("select t.* from etk_qrtz_triggers t join etk_job j on j.business_key = t.job_name where ");
        addLargeInClause("t.job_name", query, scriptObjectParamMap, jobBusinessKeys);

        addListToXml(outputStream,
                "JOBS",
                "ETK_QRTZ_TRIGGERS",
                jobBusinessKeys.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /* ETK_QRTZ_JOB_DETAILS */
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("select jd.* from ETK_QRTZ_JOB_DETAILS jd where ");
        addLargeInClause("jd.job_name", query, scriptObjectParamMap, jobBusinessKeys);

        addListToXml(outputStream,
                "JOBS",
                "ETK_QRTZ_JOB_DETAILS",
                jobBusinessKeys.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /* ETK_QRTZ_BLOB_TRIGGERS */
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("select bt.* from ETK_QRTZ_BLOB_TRIGGERS bt where bt.trigger_name in ( select t.trigger_name from etk_qrtz_triggers t where ");
        addLargeInClause("t.job_name", query, scriptObjectParamMap, jobBusinessKeys);
        query.append(")");

        addListToXml(outputStream,
                "JOBS",
                "ETK_QRTZ_BLOB_TRIGGERS",
                jobBusinessKeys.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /* ETK_QRTZ_CRON_TRIGGERS */
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("select ct.* from ETK_QRTZ_CRON_TRIGGERS ct where ct.trigger_name in ( select t.trigger_name from etk_qrtz_triggers t where ");
        addLargeInClause("t.job_name", query, scriptObjectParamMap, jobBusinessKeys);
        query.append(")");

        addListToXml(outputStream,
                "JOBS",
                "ETK_QRTZ_CRON_TRIGGERS",
                jobBusinessKeys.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());


        /* ETK_QRTZ_FIRED_TRIGGERS */
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("select ft.* from ETK_QRTZ_FIRED_TRIGGERS ft where ft.trigger_name in ( select t.trigger_name from etk_qrtz_triggers t where ");
        addLargeInClause("t.job_name", query, scriptObjectParamMap, jobBusinessKeys);
        query.append(")");

        addListToXml(outputStream,
                "JOBS",
                "ETK_QRTZ_FIRED_TRIGGERS",
                jobBusinessKeys.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /* ETK_QRTZ_SIMPLE_TRIGGERS */
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("select st.* from ETK_QRTZ_SIMPLE_TRIGGERS st where st.trigger_name in ( select t.trigger_name from etk_qrtz_triggers t where ");
        addLargeInClause("t.job_name", query, scriptObjectParamMap, jobBusinessKeys);
        query.append(")");

        addListToXml(outputStream,
                "JOBS",
                "ETK_QRTZ_SIMPLE_TRIGGERS",
                jobBusinessKeys.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /* ETK_QRTZ_SIMPROP_TRIGGERS */
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("select simpt.* from ETK_QRTZ_SIMPROP_TRIGGERS simpt where simpt.trigger_name in ( select t.trigger_name from etk_qrtz_triggers t where ");
        addLargeInClause("t.job_name", query, scriptObjectParamMap, jobBusinessKeys);
        query.append(")");

        addListToXml(outputStream,
                "JOBS",
                "ETK_QRTZ_SIMPROP_TRIGGERS",
                jobBusinessKeys.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /* ETK_QRTZ_CALENDARS */
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("select cal.* from ETK_QRTZ_CALENDARS cal where cal.calendar_name in (  select t.calendar_name from etk_qrtz_triggers t  join etk_job j on j.business_key =  t.job_name where ");
        addLargeInClause("t.job_name", query, scriptObjectParamMap, jobBusinessKeys);
        query.append(")");

        addListToXml(outputStream,
                "JOBS",
                "ETK_QRTZ_CALENDARS",
                jobBusinessKeys.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /* ETK_SUBJECT */
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append(" select subject_id, alpha_name, name, ");
        query.append(" (select code from etk_hierarchy h where h.hierarchy_id = s.hierarchy_id) hierarchy, business_key ");
        query.append(" from etk_subject s ");
        query.append(" join etk_group g on s.subject_id = g.group_id where ");
        addLargeInClause("BUSINESS_KEY", query, scriptObjectParamMap, groupBusinessKeys);
        query.append(" order by business_key ");

        addListToXml(outputStream,
                "GROUPS",
                "ETK_SUBJECT",
                groupBusinessKeys.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());
//
//        /* ETK_SUBJECT_ASSOCIATION */
//        query = new StringBuilder();
//        scriptObjectParamMap = new HashMap<String, Object>();
//        query.append(" select g.business_key as group_business_key, ");
//        query.append(" (select business_key from etk_role r where r.role_id = (select sr.ROLE_ID from ETK_SUBJECT_ROLE sr where sr.SUBJECT_ROLE_ID = s.SUBJECT_ROLE_ID)) role_business_key, ");
//        query.append(" (select u.username from etk_user u where u.user_id = (select sr.SUBJECT_ID from ETK_SUBJECT_ROLE sr where sr.SUBJECT_ROLE_ID = s.SUBJECT_ROLE_ID)) username ");
//        query.append(" from ETK_SUBJECT_ASSOCIATION s ");
//        query.append(" join etk_group g on s.subject_id = g.group_id where ");
//        addLargeInClause("BUSINESS_KEY", query, scriptObjectParamMap, groupBusinessKeys);
//        query.append(" order by business_key ");
//
//        addListToXml(outputStream,
//                "GROUPS",
//                "ETK_SUBJECT_ASSOCIATION",
//                groupBusinessKeys.size() == 0
//                ? Collections.emptyList()
//                : etk.createSQL(query.toString())
//                .setParameter(scriptObjectParamMap)
//                .fetchList());


        /* ETK_GROUP */
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("select * from etk_group where ");
        addLargeInClause("BUSINESS_KEY", query, scriptObjectParamMap, groupBusinessKeys);
        query.append(" order by group_name");

        addListToXml(outputStream,
                "GROUPS",
                "ETK_GROUP",
                groupBusinessKeys.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /* ETK_USER_GROUP_ASSOC */
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append("select (select username from etk_user u where u.user_id = assoc.user_id) username, business_key from etk_user_group_assoc assoc ");
        query.append("join etk_group g on assoc.group_id = g.group_id where ");
        addLargeInClause("BUSINESS_KEY", query, scriptObjectParamMap, groupBusinessKeys);
        query.append(" order by group_name");

        addListToXml(outputStream,
                "GROUPS",
                "ETK_USER_GROUP_ASSOC",
                groupBusinessKeys.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /* ETK_SUBJECT_ROLE */
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append(" select g.business_key as GROUP_BUSINESS_KEY, r.business_key ROLE_BUSINESS_KEY from ETK_SUBJECT_ROLE sr ");
        query.append(" join etk_role r on r.role_id = sr.role_id ");
        query.append(" join etk_group g on g.group_id = sr.subject_id where ");
        addLargeInClause("g.business_key", query, scriptObjectParamMap, groupBusinessKeys);
        query.append(" order by g.business_key");

        addListToXml(outputStream,
                "GROUPS",
                "ETK_SUBJECT_ROLE",
                groupBusinessKeys.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /* ETK_SUBJECT_PREFERENCE */
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append(" select sp.* from etk_subject_preference sp ");
        query.append(" join etk_group g on g.group_id = sp.subject_id where ");
        addLargeInClause("g.business_key", query, scriptObjectParamMap, groupBusinessKeys);
        query.append(" order by g.business_key");

        addListToXml(outputStream,
                "GROUPS",
                "ETK_SUBJECT_PREFERENCE",
                groupBusinessKeys.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());


        /* ETK_INBOX_PREFERENCE */
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append(" select ip.* from ETK_INBOX_PREFERENCE ip ");
        query.append(" join etk_subject_preference sp on sp.SUBJECT_PREFERENCE_ID = ip.SUBJECT_PREFERENCE_ID ");
        query.append(" join etk_group g on g.group_id = sp.subject_id where ");
        addLargeInClause("g.business_key", query, scriptObjectParamMap, groupBusinessKeys);
        query.append(" order by g.business_key");

        addListToXml(outputStream,
                "GROUPS",
                "ETK_INBOX_PREFERENCE",
                groupBusinessKeys.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());

        /* ETK_STATE_FILTER */
        query = new StringBuilder();
        scriptObjectParamMap = new HashMap<>();
        query.append(" select sf.* from ETK_STATE_FILTER sf ");
        query.append(" join ETK_INBOX_PREFERENCE ip on sf.INBOX_PREFERENCE_ID = ip.INBOX_PREFERENCE_ID ");
        query.append(" join etk_subject_preference sp on sp.SUBJECT_PREFERENCE_ID = ip.SUBJECT_PREFERENCE_ID ");
        query.append(" join etk_group g on g.group_id = sp.subject_id where ");
        addLargeInClause("g.business_key", query, scriptObjectParamMap, groupBusinessKeys);
        query.append(" order by g.business_key");

        addListToXml(outputStream,
                "GROUPS",
                "ETK_STATE_FILTER",
                groupBusinessKeys.size() == 0
                ? Collections.emptyList()
                : etk.createSQL(query.toString())
                .setParameter(scriptObjectParamMap)
                .fetchList());
    }

    /**
     * Converts a list of data permissions so that the data permission values refer to the display value of the
     * permission instead of the integer value of the permission.
     *
     * Destructively modifies the input list and returns it.
     *
     * @param dataPermissions The permissions with the value referencing the entellitrak value
     * @return The permissions with the value referencing the display value
     */
    private static List<Map<String, Object>> convertAccessLevelsFromNumberToDisplay(final List<Map<String, Object>> dataPermissions) {
        final String[] attributes = new String[]{
                "CREATE",
                "READ",
                "UPDATE",
                "DELETE",
                "ASSIGN",
                "SEARCH",
                "REPORTING",
                "READ_CONTENT"};

        for(final Map<String, Object> dataPermission : dataPermissions){
            for(final String attribute : attributes){
                dataPermission.put(attribute,
                        AccessLevel.getByEntellitrakNumber(((Number) dataPermission.get(attribute)).intValue()).getDisplay());
            }
        }

        return dataPermissions;
    }

    /**
     * This method returns the response to be passed to the view code.
     *
     * @return the page response
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     * @throws IOException If there was an underlying {@link IOException}
     * @throws ParserConfigurationException If there was an underlying {@link ParserConfigurationException}
     * @throws TransformerException If there was an underlying {@link TransformerException}
     */
    private Response exportObjects() throws IncorrectResultSizeDataAccessException,
                                           IOException,
                                           ParserConfigurationException,
                                           TransformerException {
        final TextResponse response = etk.createTextResponse();
        final Parameters request = etk.getParameters();

        /*Our 2 requested actions are going to be initial and generateXml, we're not really going to worry about error handling in this page*/
        String requestedAction = request.getSingle("requestedAction");
        if(StringUtility.isBlank(requestedAction)){
            requestedAction = "initial";
        }

        /*Here is where we actually determine whether this is the first time viewing the page, or whether they are submitting the form*/

        if ("initial".equals(requestedAction)) {
            response.put("baseTrackedObjects", etk.createSQL("SELECT do.BUSINESS_KEY FROM etk_data_object do "
                    + "WHERE do.tracking_config_id = :trackingConfigId AND base_object = 1 AND object_type = 1 "
                    + "ORDER BY do.business_key")
                    .setParameter("trackingConfigId", nextTrackingConfigId)
                    .fetchJSON());
            response.put("referenceObjects", etk.createSQL("SELECT do.BUSINESS_KEY FROM etk_data_object do "
                    + "WHERE do.tracking_config_id = :trackingConfigId AND object_type = 2 "
                    + "ORDER BY do.business_key")
                    .setParameter("trackingConfigId", nextTrackingConfigId)
                    .fetchJSON());
            response.put("etkPageObjects", etk.createSQL("SELECT page.NAME, page.BUSINESS_KEY FROM etk_page page "
                    + "ORDER BY page.NAME")
                    .fetchJSON());
            response.put("scriptObjects", etk.createSQL(
                    "SELECT SCRIPT_BUSINESS_KEY as BUSINESS_KEY, FULLY_QUALIFIED_SCRIPT_NAME AS NAME "
                            + "FROM AEA_SCRIPT_PKG_VIEW_SYS_ONLY "
                            + "ORDER BY NAME")
                            .fetchJSON());
            response.put("componentObjects", etk.createSQL(
                    "SELECT c_code as BUSINESS_KEY, c_name AS NAME, c_current_version as VERSION "
                            + "FROM T_AEA_COMPONENT_VERSION_INFO "
                            + "ORDER BY NAME")
                            .fetchJSON());
            response.put("roleObjects", etk.createSQL("SELECT role.NAME, role.BUSINESS_KEY FROM ETK_ROLE role "
                    + "ORDER BY role.NAME")
                    .fetchJSON());
            response.put("groupObjects", etk.createSQL("SELECT GROUP_NAME AS NAME, BUSINESS_KEY FROM ETK_GROUP "
                    + "ORDER BY GROUP_NAME")
                    .fetchJSON());
            response.put("jobObjects", etk.createSQL("SELECT sj.NAME, sj.BUSINESS_KEY FROM ETK_JOB sj "
                    + "ORDER BY sj.NAME")
                    .fetchJSON());

            response.setContentType(ContentType.HTML);
            response.put("form", "initial");
        } else if ("generateXml".equals(requestedAction)) {

            List<String> dataObjectBusinessKeys = request.getField("objectKeys");
            List<String> scriptObjectBusinessKeys = request.getField("scriptObjectKeys");
            List<String> etkPageObjectBusinessKeys = request.getField("etkPageKeys");
            List<String> roleBusinessKeys = request.getField("roleBusinessKeys");
            List<String> groupBusinessKeys = request.getField("groupBusinessKeys");
            List<String> jobBusinessKeys = request.getField("jobBusinessKeys");
            List<String> selectedComponentCCodes = request.getField("selectedComponents");

            if(null == dataObjectBusinessKeys){
                dataObjectBusinessKeys = new ArrayList<>();
            }
            if(null == scriptObjectBusinessKeys){
                scriptObjectBusinessKeys = new ArrayList<>();
            }
            if(null == etkPageObjectBusinessKeys){
                etkPageObjectBusinessKeys = new ArrayList<>();
            }
            if(null == roleBusinessKeys){
                roleBusinessKeys = new ArrayList<>();
            }
            if(null == groupBusinessKeys){
                groupBusinessKeys = new ArrayList<>();
            }
            if(null == jobBusinessKeys){
                jobBusinessKeys = new ArrayList<>();
            }
            if(null == selectedComponentCCodes){
                selectedComponentCCodes = new ArrayList<>();
            }

            final Set<BigDecimal> dataObjectIds = new TreeSet<>(); //This will contain the IDS for the entire tree of objects which need to be copied
            final Set<BigDecimal> scriptObjectIds = new TreeSet<>(); //This will contain the IDS for the script objects the user wants copied
            final Set<BigDecimal> etkPageObjectIds = new TreeSet<>();

            for(final String dataObjectBusinessKey : dataObjectBusinessKeys){
                dataObjectIds.add((BigDecimal) etk.createSQL("SELECT do.DATA_OBJECT_ID FROM etk_data_object do "
                        + "WHERE do.tracking_config_id = :trackingConfigId "
                        + "AND do.business_key = :dataObjectBusinessKey")
                        .setParameter("trackingConfigId", nextTrackingConfigId)
                        .setParameter("dataObjectBusinessKey", dataObjectBusinessKey)
                        .fetchObject());
            }

            for(final String scriptObjectBusinessKey : scriptObjectBusinessKeys){
                scriptObjectIds.add((BigDecimal) etk.createSQL("SELECT so.SCRIPT_ID FROM etk_script_object so "
                        + "WHERE so.tracking_config_id = :trackingConfigId "
                        + "AND so.workspace_id = :workspaceId "
                        + "AND so.business_key = :businessKey")
                        .setParameter("workspaceId", workspaceId)
                        .setParameter("trackingConfigId", nextTrackingConfigId)
                        .setParameter("businessKey", scriptObjectBusinessKey)
                        .fetchObject());
            }

            for(final String etkPageObjectBusinessKey : etkPageObjectBusinessKeys){
                etkPageObjectIds.add((BigDecimal) etk.createSQL("SELECT page.PAGE_ID FROM etk_page page "
                        + "WHERE page.business_key = :businessKey")
                        .setParameter("workspaceId", workspaceId)
                        .setParameter("trackingConfigId", nextTrackingConfigId)
                        .setParameter("businessKey", etkPageObjectBusinessKey)
                        .fetchObject());
            }

            ZipOutputStream outputStream = null;

            try{
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                outputStream = new ZipOutputStream(baos);

                generateXml(dataObjectIds,
                        scriptObjectIds,
                        etkPageObjectIds,
                        roleBusinessKeys,
                        groupBusinessKeys,
                        jobBusinessKeys,
                        selectedComponentCCodes,
                        outputStream);


                if ((scriptObjectIds != null) && (scriptObjectIds.size() > 0)) {

                	final List<BigDecimal> scriptObjectIdList = new ArrayList<>();
                	scriptObjectIdList.addAll(scriptObjectIds);

                	final Map<String, Object> scriptObjectParamMap = new HashMap<>();
                	final StringBuilder query = new StringBuilder();
                	query.append("select sov.SCRIPT_NAME, sov.PACKAGE_PATH, eso.CODE as SCRIPT_CODE, eso.LANGUAGE_TYPE from AEA_SCRIPT_PKG_VIEW_SYS_ONLY sov JOIN ETK_SCRIPT_OBJECT eso on eso.SCRIPT_ID = sov.SCRIPT_ID WHERE ");
                	addLargeInClause("eso.SCRIPT_ID", query, scriptObjectParamMap, scriptObjectIdList);

                	final List<Map<String, Object>> scriptObjects =
                            etk.createSQL(query.toString())
                            .setParameter(scriptObjectParamMap)
                            .fetchList();

                    for (final Map<String, Object> aScript : scriptObjects) {

                        final String packageZipRelativePath = aScript.get("PACKAGE_PATH") == null
                                ? ""
                                  : ((String) aScript.get("PACKAGE_PATH")).replace('.', '/') + "/";

                        final String path = "SCRIPT_OBJECTS/" + packageZipRelativePath
                                + ((String) aScript.get("SCRIPT_NAME"))
                                + "."
                                + LanguageType.fromEntellitrakNumber(((Number) aScript.get("LANGUAGE_TYPE")).intValue()).getFileExtension();

                        outputStream.putNextEntry(new ZipEntry(path));

                        final byte[] scriptCodeBytes = Optional.ofNullable((String) aScript.get("SCRIPT_CODE"))
                             .orElse("")
                             .getBytes();

                        outputStream.write(scriptCodeBytes);
                        outputStream.closeEntry();
                    }
                }

                outputStream.finish();
                outputStream.flush();
                baos.flush();

                final byte[] outputBytes = baos.toByteArray();

                final String currentDateTime = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());

                final String currentVersion =
                        etk.createSQL("select CURRENT_VERSION from ETK_INSTALLED_RELEASES").fetchString();

                return etk.createFileResponse("aea_export_v"
                        + currentVersion
                        + "_"
                        + currentDateTime
                        + ".zip", outputBytes);

            }finally{
                IOUtility.closeQuietly(outputStream);
            }
        }

        return response;
    }

    /**
     * Is Sql Server.
     *
     * @return is the DB sqlServer?
     */
    private boolean isSqlServer(){
        return com.entellitrak.platform.DatabasePlatform.SQL_SERVER.equals(etk.getPlatformInfo().getDatabasePlatform());
    }

    /**
     * Add an in clause to an SQL string builder that supports more than 1000 records.
     *
     * inObjectList is split into a bracketed set of multiple groups:
     *
     * (columnName in (:inObjectList0-500)
     *  or columnName in (:inObjectList501-1000)
     *  or columnName in (:inObjectList1001-1500))
     *
     *  The resulting SQL is inserted directly into the provided queryBuilder.
     *
     * @param columnName The column name to compare in the in clause.
     * @param queryBuilder The query to insert the in clause into.
     * @param outputParamMap The parameter map that will be passed into the query.
     * @param inObjectList The list of objects to insert into the in(:objects) clause.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void addLargeInClause (final String columnName, final StringBuilder queryBuilder,
            final Map outputParamMap, final List inObjectList) {
        final int groupSize = 1000;
        final String noPeriodColumnName = columnName.replaceAll("\\.", "_");

        queryBuilder.append(" (");

        if ((inObjectList == null) || (inObjectList.size() == 0)) {
            queryBuilder.append(columnName);
            queryBuilder.append(" in (null)");
        } else if (inObjectList.size() == 1) {
        	queryBuilder.append(columnName);
        	queryBuilder.append(" = :");
        	queryBuilder.append(noPeriodColumnName);
        	outputParamMap.put(noPeriodColumnName, inObjectList.get(0));
        } else {
            int paramGroup = 0;

            for (int i = 0; i < inObjectList.size(); i=i+groupSize) {
                if ((i + groupSize) < inObjectList.size()) {
                    queryBuilder.append(columnName);
                    queryBuilder.append(" in (:" + noPeriodColumnName + paramGroup + ") OR ");
                    outputParamMap.put(noPeriodColumnName + paramGroup, inObjectList.subList(i, i+groupSize));
                } else {
                    queryBuilder.append(columnName);
                    queryBuilder.append(" in (:" + noPeriodColumnName + paramGroup + ")");
                    outputParamMap.put(noPeriodColumnName + paramGroup, inObjectList.subList(i, inObjectList.size()));
                }
                paramGroup++;
            }
        }

        queryBuilder.append(") ");
    }
}